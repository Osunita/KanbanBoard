/** Task create/edit dialog: Signal Forms `form()` inside a native `<dialog>` (DEC-018, F7). */
import { Component, computed, input, output, signal, viewChild } from '@angular/core';
import { FormField, form, maxLength, required, validate } from '@angular/forms/signals';
import type { Task, TaskPriority } from '../../core/models/task.model';

/** Payload emitted to the parent, which triggers the store action (§5.2). */
export interface TaskFormValue {
  title: string;
  description: string | null;
  priority: TaskPriority;
}

const REQUIRED_MESSAGE = 'Este campo es obligatorio.';

@Component({
  selector: 'app-task-form-dialog',
  imports: [FormField],
  styleUrl: './task-form-dialog.css',
  templateUrl: './task-form-dialog.html',
})
export class TaskFormDialog {
  /** True while the parent's create/update request is in flight (§8.6, E-21). */
  readonly saving = input(false);

  readonly save = output<TaskFormValue>();

  private readonly dialogElement = viewChild.required<HTMLDialogElement>('dialogElement');
  private readonly editing = signal<Task | null>(null);
  private readonly model = signal<{ title: string; description: string; priority: TaskPriority }>({
    title: '',
    description: '',
    priority: 'MEDIUM',
  });

  protected readonly taskForm = form(this.model, (m) => {
    required(m.title, { message: REQUIRED_MESSAGE });
    // The backend rejects a blank title after trim (SPEC §7.3, E-04);
    // signal-forms `required` only rejects '', so catch whitespace-only too.
    validate(m.title, ({ value }) =>
      value() !== '' && value().trim() === ''
        ? { kind: 'required', message: REQUIRED_MESSAGE }
        : undefined,
    );
    maxLength(m.title, 200, { message: 'Máximo 200 caracteres.' });
    maxLength(m.description, 2000, { message: 'Máximo 2000 caracteres.' });
    required(m.priority, { message: REQUIRED_MESSAGE });
  });

  protected readonly isEditing = computed(() => this.editing() !== null);

  /** Always-rendered dialog (F7): reset the model, then `showModal()`. */
  open(task: Task | null): void {
    this.editing.set(task);
    this.model.set({
      title: task?.title ?? '',
      description: task?.description ?? '',
      priority: task?.priority ?? 'MEDIUM',
    });
    this.dialogElement().showModal();
  }

  close(): void {
    this.dialogElement().close();
  }

  protected submit(): void {
    if (this.taskForm().invalid() || this.saving()) {
      return;
    }
    const value = this.taskForm().value();
    const title = value.title.trim();
    const description = value.description.trim();
    this.save.emit({
      title,
      description: description === '' ? null : description,
      priority: value.priority,
    });
  }
}
