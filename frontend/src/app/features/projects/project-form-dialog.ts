/** Project create/edit dialog: Signal Forms `form()` inside a native `<dialog>` (DEC-018, F7). */
import { Component, computed, input, output, signal, viewChild } from '@angular/core';
import { FormField, form, maxLength, required, validate } from '@angular/forms/signals';
import type { Project } from '../../core/models/project.model';

/** Payload emitted to the parent, which performs the HTTP request (§5.2). */
export interface ProjectFormValue {
  name: string;
  description: string | null;
}

const REQUIRED_MESSAGE = 'Este campo es obligatorio.';

@Component({
  selector: 'app-project-form-dialog',
  imports: [FormField],
  styleUrl: './project-form-dialog.css',
  templateUrl: './project-form-dialog.html',
})
export class ProjectFormDialog {
  /** True while the parent's create/update request is in flight (E-21). */
  readonly saving = input(false);

  readonly save = output<ProjectFormValue>();

  private readonly dialogElement = viewChild.required<HTMLDialogElement>('dialogElement');
  private readonly editing = signal<Project | null>(null);
  private readonly model = signal<{ name: string; description: string }>({
    name: '',
    description: '',
  });

  protected readonly projectForm = form(this.model, (m) => {
    required(m.name, { message: REQUIRED_MESSAGE });
    // The backend treats a blank name as invalid after trim (SPEC §7.3, E-04);
    // signal-forms `required` only rejects '', so catch whitespace-only too.
    validate(m.name, ({ value }) =>
      value() !== '' && value().trim() === ''
        ? { kind: 'required', message: REQUIRED_MESSAGE }
        : undefined,
    );
    maxLength(m.name, 100, { message: 'Máximo 100 caracteres.' });
    maxLength(m.description, 1000, { message: 'Máximo 1000 caracteres.' });
  });

  protected readonly isEditing = computed(() => this.editing() !== null);

  /** Always-rendered dialog (F7): reset the model, then `showModal()`. */
  open(project: Project | null): void {
    this.editing.set(project);
    this.model.set({ name: project?.name ?? '', description: project?.description ?? '' });
    this.dialogElement().showModal();
  }

  close(): void {
    this.dialogElement().close();
  }

  protected submit(): void {
    if (this.projectForm().invalid() || this.saving()) {
      return;
    }
    const value = this.projectForm().value();
    const name = value.name.trim();
    const description = value.description.trim();
    this.save.emit({ name, description: description === '' ? null : description });
  }
}
