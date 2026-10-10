/** Projects list screen (SPEC §8.3): httpResource list, four states, CRUD dialogs. */
import { Component, computed, ElementRef, inject, signal, viewChild } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { ProjectApi } from '../../core/api/project-api';
import { errorMessage } from '../../core/error/error-message';
import type { Project } from '../../core/models/project.model';
import { ProjectFormDialog, type ProjectFormValue } from './project-form-dialog';

@Component({
  selector: 'app-project-list-page',
  imports: [RouterLink, ProjectFormDialog],
  styleUrl: './project-list-page.css',
  templateUrl: './project-list-page.html',
})
export class ProjectListPage {
  private readonly api = inject(ProjectApi);

  /** DEC-014/F5: `httpResource` for the list only, `reload()` after mutations. */
  protected readonly projects = httpResource<Project[]>(() => '/api/projects');

  protected readonly list = computed(() => {
    if (this.projects.status() === 'error') {
      return [];
    }
    return this.projects.value() ?? [];
  });

  protected readonly notice = signal<string | null>(null);
  protected readonly saving = signal(false);
  protected readonly deleting = signal(false);
  private readonly editingProject = signal<Project | null>(null);
  private readonly pendingDelete = signal<Project | null>(null);

  private readonly formDialog = viewChild.required<ProjectFormDialog>('formDialog');
  /** Template ref query returns an ElementRef (string refs read ElementRef by default). */
  private readonly deleteDialog = viewChild.required<ElementRef<HTMLDialogElement>>('deleteDialog');

  newProject(): void {
    this.editingProject.set(null);
    this.formDialog().open(null);
  }

  edit(project: Project): void {
    this.editingProject.set(project);
    this.formDialog().open(project);
  }

  saveProject(value: ProjectFormValue): void {
    if (this.saving()) {
      return;
    }
    const editing = this.editingProject();
    this.saving.set(true);
    const request$ = editing === null ? this.api.create(value) : this.api.update(editing.id, value);
    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.formDialog().close();
        this.projects.reload();
      },
      error: (err: unknown) => {
        this.saving.set(false);
        this.formDialog().close();
        this.notice.set(errorMessage(err));
      },
    });
  }

  requestDelete(project: Project): void {
    this.pendingDelete.set(project);
    this.deleteDialog().nativeElement.showModal();
  }

  cancelDelete(): void {
    this.deleteDialog().nativeElement.close();
    this.pendingDelete.set(null);
  }

  confirmDelete(): void {
    const project = this.pendingDelete();
    if (project === null || this.deleting()) {
      return;
    }
    this.deleting.set(true);
    this.api.remove(project.id).subscribe({
      next: () => {
        this.deleting.set(false);
        this.deleteDialog().nativeElement.close();
        this.pendingDelete.set(null);
        this.projects.reload();
      },
      error: (err: unknown) => {
        this.deleting.set(false);
        this.deleteDialog().nativeElement.close();
        this.pendingDelete.set(null);
        this.notice.set(errorMessage(err));
      },
    });
  }

  retry(): void {
    this.projects.reload();
  }

  dismissNotice(): void {
    this.notice.set(null);
  }
}
