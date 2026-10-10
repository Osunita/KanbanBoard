/** Board screen (SPEC §8.4–8.6): route id, project header, columns, dialogs and states. */
import { Component, computed, inject, input, OnInit, signal, viewChild } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProjectApi } from '../../core/api/project-api';
import { isNotFound } from '../../core/error/error-message';
import type { Project } from '../../core/models/project.model';
import type { Task } from '../../core/models/task.model';
import { BoardColumn, type TaskDrop } from './board-column';
import { BoardStore } from './board.store';
import { TaskFormDialog, type TaskFormValue } from './task-form-dialog';

@Component({
  selector: 'app-board-page',
  imports: [RouterLink, BoardColumn, TaskFormDialog],
  styleUrl: './board-page.css',
  templateUrl: './board-page.html',
})
export class BoardPage implements OnInit {
  /** Route param `:id`, bound as component input (`withComponentInputBinding`). */
  readonly id = input.required<string>();

  protected readonly store = inject(BoardStore);
  private readonly projectApi = inject(ProjectApi);

  protected readonly project = signal<Project | null>(null);
  private readonly projectLoading = signal(false);
  private readonly projectError = signal(false);
  private readonly projectNotFound = signal(false);

  private readonly editingTask = signal<Task | null>(null);
  private readonly pendingDelete = signal<Task | null>(null);

  private readonly formDialog = viewChild.required<TaskFormDialog>('formDialog');
  private readonly deleteDialog = viewChild.required<HTMLDialogElement>('deleteDialog');

  ngOnInit(): void {
    this.load();
  }

  /** Five board states (SPEC §8.4): not found → load error → loading → data. */
  protected readonly notFound = computed(() => this.projectNotFound() || this.store.notFound());
  protected readonly loadError = computed(
    () => !this.notFound() && (this.projectError() || this.store.error()),
  );
  protected readonly loading = computed(
    () =>
      !this.notFound() && !this.loadError() && (this.projectLoading() || this.store.loading()),
  );

  private load(): void {
    const projectId = Number(this.id());
    if (!Number.isInteger(projectId)) {
      this.projectNotFound.set(true);
      return;
    }
    this.project.set(null);
    this.projectLoading.set(true);
    this.projectError.set(false);
    this.projectNotFound.set(false);
    this.projectApi.get(projectId).subscribe({
      next: (project) => {
        this.project.set(project);
        this.projectLoading.set(false);
      },
      error: (err: unknown) => {
        this.projectLoading.set(false);
        if (isNotFound(err)) {
          this.projectNotFound.set(true);
        } else {
          this.projectError.set(true);
        }
      },
    });
    this.store.load(projectId);
  }

  protected retry(): void {
    this.load();
  }

  protected newTask(): void {
    this.editingTask.set(null);
    this.formDialog().open(null);
  }

  protected editTask(task: Task): void {
    this.editingTask.set(task);
    this.formDialog().open(task);
  }

  protected saveTask(value: TaskFormValue): void {
    const editing = this.editingTask();
    const close = (): void => this.formDialog().close();
    if (editing === null) {
      this.store.create(value, close);
    } else {
      this.store.edit(editing.id, value, close);
    }
  }

  protected requestDelete(task: Task): void {
    this.pendingDelete.set(task);
    this.deleteDialog().showModal();
  }

  protected cancelDelete(): void {
    this.deleteDialog().close();
    this.pendingDelete.set(null);
  }

  protected confirmDelete(): void {
    const task = this.pendingDelete();
    if (task === null || this.store.saving()) {
      return;
    }
    this.store.delete(task.id, () => {
      this.deleteDialog().close();
      this.pendingDelete.set(null);
    });
  }

  protected onDrop(drop: TaskDrop): void {
    this.store.move(drop.taskId, drop.targetStatus, drop.targetIndex);
  }

  protected dismissActionError(): void {
    this.store.dismissActionError();
  }
}
