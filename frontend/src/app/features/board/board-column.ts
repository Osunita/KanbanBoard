/** One board column: title, counter and a connected CDK drop list (DEC-013, F2/F3). */
import { Component, computed, input, output } from '@angular/core';
import { CdkDrag, type CdkDragDrop, CdkDropList } from '@angular/cdk/drag-drop';
import { TASK_STATUSES, type Task, type TaskStatus } from '../../core/models/task.model';
import { TaskCard } from './task-card';

/** Domain drop event forwarded to `BoardStore.move` (SPEC §8.5). */
export interface TaskDrop {
  readonly taskId: number;
  readonly targetStatus: TaskStatus;
  readonly targetIndex: number;
}

/** F2: stable drop-list ids, each column connected to the other two. */
const DROP_LIST_IDS: readonly string[] = TASK_STATUSES.map((status) => `col-${status}`);

@Component({
  selector: 'app-board-column',
  imports: [CdkDropList, CdkDrag, TaskCard],
  styleUrl: './board-column.css',
  templateUrl: './board-column.html',
})
export class BoardColumn {
  readonly status = input.required<TaskStatus>();
  readonly tasks = input.required<Task[]>();

  readonly dropped = output<TaskDrop>();
  readonly edit = output<Task>();
  readonly remove = output<Task>();

  protected readonly listId = computed(() => `col-${this.status()}`);
  protected readonly connectedTo = computed(() =>
    DROP_LIST_IDS.filter((id) => id !== this.listId()),
  );
  protected readonly count = computed(() => this.tasks().length);

  /** F3: emit task id + target status + `currentIndex` (matches SPEC §7.4 index semantics). */
  protected onDropped(event: CdkDragDrop<Task[]>): void {
    const task: Task = event.item.data;
    this.dropped.emit({
      taskId: task.id,
      targetStatus: this.status(),
      targetIndex: event.currentIndex,
    });
  }
}
