import { Injectable, computed, inject, signal } from '@angular/core';
import { TaskApi } from '../../core/api/task-api';
import { errorMessage, isNotFound } from '../../core/error/error-message';
import {
  TASK_STATUSES,
  type Task,
  type TaskCreateRequest,
  type TaskStatus,
  type TaskUpdateRequest,
} from '../../core/models/task.model';
import { moveTask } from './board.utils';

/** One board column: a status and its tasks sorted by position (SPEC §8.4). */
export interface BoardColumn {
  readonly status: TaskStatus;
  readonly tasks: Task[];
}

/** Invoked when a create/edit/delete request settles, success or error (E-21). */
export type SettledCallback = () => void;

const byPosition = (a: Task, b: Task): number => a.position - b.position;

/**
 * Board state container (DEC-011, §5.2): signals plus the actions of the board
 * screen. HTTP access goes through `core/api` only; move follows SPEC §8.5 —
 * snapshot, optimistic `moveTask`, PATCH, silent reconcile, rollback on error.
 */
@Injectable({ providedIn: 'root' })
export class BoardStore {
  private readonly api = inject(TaskApi);
  private projectId: number | null = null;

  readonly tasks = signal<Task[]>([]);
  readonly loading = signal(false);
  readonly error = signal(false);
  readonly notFound = signal(false);
  readonly actionError = signal<string | null>(null);
  readonly saving = signal(false);

  /** Columns always in TODO → IN_PROGRESS → DONE order, tasks by position (SPEC §8.4). */
  readonly columns = computed<BoardColumn[]>(() =>
    TASK_STATUSES.map((status) => ({
      status,
      tasks: this.tasks().filter((task) => task.status === status).sort(byPosition),
    })),
  );

  load(projectId: number): void {
    this.projectId = projectId;
    this.tasks.set([]);
    this.loading.set(true);
    this.error.set(false);
    this.notFound.set(false);
    this.actionError.set(null);
    this.api.list(projectId).subscribe({
      next: (tasks) => {
        this.tasks.set(tasks);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.tasks.set([]);
        this.loading.set(false);
        if (isNotFound(err)) {
          this.notFound.set(true);
        } else {
          this.error.set(true);
        }
      },
    });
  }

  create(value: TaskCreateRequest, onSettled?: SettledCallback): void {
    if (this.saving() || this.projectId === null) {
      return;
    }
    this.saving.set(true);
    this.actionError.set(null);
    this.api.create(this.projectId, value).subscribe({
      next: (task) => {
        this.saving.set(false);
        this.tasks.update((list) => [...list, task]);
        onSettled?.();
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.actionError.set(errorMessage(err));
        onSettled?.();
      },
    });
  }

  edit(id: number, value: TaskUpdateRequest, onSettled?: SettledCallback): void {
    if (this.saving()) {
      return;
    }
    this.saving.set(true);
    this.actionError.set(null);
    this.api.update(id, value).subscribe({
      next: (updated) => {
        this.saving.set(false);
        this.tasks.update((list) => list.map((task) => (task.id === id ? updated : task)));
        onSettled?.();
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.actionError.set(errorMessage(err));
        onSettled?.();
      },
    });
  }

  delete(id: number, onSettled?: SettledCallback): void {
    if (this.saving()) {
      return;
    }
    const removed = this.tasks().find((task) => task.id === id);
    this.saving.set(true);
    this.actionError.set(null);
    this.api.remove(id).subscribe({
      next: () => {
        this.saving.set(false);
        this.tasks.update((list) => {
          const survivors = list.filter((task) => task.id !== id);
          if (removed === undefined) {
            return survivors;
          }
          // Renumber the source column 0..n-1, mirroring the server (SPEC §7.1).
          let position = 0;
          return survivors.map((task) =>
            task.status === removed.status ? { ...task, position: position++ } : task,
          );
        });
        onSettled?.();
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.actionError.set(errorMessage(err));
        onSettled?.();
      },
    });
  }

  /**
   * SPEC §8.5: snapshot → optimistic `moveTask` → PATCH {status, position};
   * success → silent GET reconcile (no spinner); error → rollback + alert.
   * Same status and index → no state change and no request (step 7, E-09).
   */
  move(taskId: number, targetStatus: TaskStatus, targetIndex: number): void {
    const snapshot = this.tasks();
    const task = snapshot.find((candidate) => candidate.id === taskId);
    if (!task) {
      return;
    }
    const sourceIndex = snapshot
      .filter((candidate) => candidate.status === task.status)
      .sort(byPosition)
      .findIndex((candidate) => candidate.id === taskId);
    if (task.status === targetStatus && sourceIndex === targetIndex) {
      return;
    }

    this.actionError.set(null);
    this.tasks.set(moveTask(snapshot, taskId, targetStatus, targetIndex));

    this.api.move(taskId, { status: targetStatus, position: targetIndex }).subscribe({
      next: () => this.reconcile(),
      error: (err: unknown) => {
        this.tasks.set(snapshot);
        this.actionError.set(errorMessage(err));
      },
    });
  }

  dismissActionError(): void {
    this.actionError.set(null);
  }

  /** Re-read the board after a successful move without toggling `loading` (§8.5). */
  private reconcile(): void {
    if (this.projectId === null) {
      return;
    }
    this.api.list(this.projectId).subscribe({
      next: (tasks) => this.tasks.set(tasks),
      // The PATCH already succeeded: keep the optimistic state rather than
      // raising a second error; the next load reconciles (E-20).
      error: () => {},
    });
  }
}
