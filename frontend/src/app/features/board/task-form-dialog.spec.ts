/** TEST-020 — task dialog validation (SPEC §12.2, board R5). */
import { TestBed } from '@angular/core/testing';
import type { Task } from '../../core/models/task.model';
import { TaskFormDialog, type TaskFormValue } from './task-form-dialog';

const existingTask: Task = {
  id: 7,
  projectId: 1,
  title: 'Existente',
  description: 'descripción previa',
  status: 'TODO',
  priority: 'HIGH',
  position: 0,
  createdAt: '2026-10-10T09:00:00.000Z',
  updatedAt: '2026-10-10T09:00:00.000Z',
};

const button = (element: HTMLElement, label: string): HTMLButtonElement | undefined =>
  Array.from(element.querySelectorAll('button')).find(
    (candidate) => candidate.textContent?.trim() === label,
  );

describe('TaskFormDialog', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TaskFormDialog] }).compileComponents();
  });

  it('TEST-020: empty title shows Este campo es obligatorio. and disables Guardar', async () => {
    const fixture = TestBed.createComponent(TaskFormDialog);
    fixture.detectChanges();
    fixture.componentInstance.open(null);
    await fixture.whenStable();
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Este campo es obligatorio.');
    expect(button(element, 'Guardar')?.disabled).toBe(true);
  });

  it('enables Guardar once a valid title is typed', async () => {
    const fixture = TestBed.createComponent(TaskFormDialog);
    fixture.detectChanges();
    fixture.componentInstance.open(null);
    await fixture.whenStable();
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const title = element.querySelector<HTMLInputElement>('#task-title');
    expect(title).toBeTruthy();
    title!.value = 'Nueva tarea';
    title!.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(element.textContent).not.toContain('Este campo es obligatorio.');
    expect(button(element, 'Guardar')?.disabled).toBe(false);
  });

  it('prefills for edit mode and resets when reopened for creation', async () => {
    const fixture = TestBed.createComponent(TaskFormDialog);
    fixture.detectChanges();

    fixture.componentInstance.open(existingTask);
    await fixture.whenStable();
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector<HTMLInputElement>('#task-title')?.value).toBe('Existente');
    expect(element.querySelector<HTMLSelectElement>('#task-priority')?.value).toBe('HIGH');
    expect(element.textContent).not.toContain('Este campo es obligatorio.');

    fixture.componentInstance.open(null);
    fixture.detectChanges();

    expect(element.querySelector<HTMLInputElement>('#task-title')?.value).toBe('');
    expect(element.querySelector<HTMLSelectElement>('#task-priority')?.value).toBe('MEDIUM');
    expect(element.textContent).toContain('Este campo es obligatorio.');
  });

  it('emits the trimmed form value on save', async () => {
    const fixture = TestBed.createComponent(TaskFormDialog);
    fixture.detectChanges();
    fixture.componentInstance.open(null);
    await fixture.whenStable();
    fixture.detectChanges();

    const emitted: TaskFormValue[] = [];
    fixture.componentInstance.save.subscribe((value) => emitted.push(value));

    const element = fixture.nativeElement as HTMLElement;
    const title = element.querySelector<HTMLInputElement>('#task-title');
    title!.value = '  Nueva tarea  ';
    title!.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    button(element, 'Guardar')?.click();
    fixture.detectChanges();

    expect(emitted).toEqual([
      { title: 'Nueva tarea', description: null, priority: 'MEDIUM' },
    ]);
  });
});
