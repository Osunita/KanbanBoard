/** Delta scenario — task card rendering: priority badge labels (board-screen "Card rendering"). */
import { TestBed } from '@angular/core/testing';
import type { Task } from '../../core/models/task.model';
import { TaskCard } from './task-card';

const button = (element: HTMLElement, label: string): HTMLButtonElement | undefined =>
  Array.from(element.querySelectorAll('button')).find(
    (candidate) => candidate.textContent?.trim() === label,
  );

const makeTask = (priority: Task['priority'], description: string | null): Task => ({
  id: 1,
  projectId: 1,
  title: 'Escribir README',
  description,
  status: 'TODO',
  priority,
  position: 0,
  createdAt: '2026-10-10T09:00:00.000Z',
  updatedAt: '2026-10-10T09:00:00.000Z',
});

describe('TaskCard', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [TaskCard] }).compileComponents();
  });

  it('shows the Alta badge for a HIGH priority task and renders the clamped description element', () => {
    const fixture = TestBed.createComponent(TaskCard);
    fixture.componentRef.setInput(
      'task',
      makeTask('HIGH', 'Descripción larga que el CSS recorta a dos líneas.'),
    );
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const badge = element.querySelector('.badge');
    expect(badge?.textContent?.trim()).toBe('Alta');
    expect(badge?.classList.contains('badge-alta')).toBe(true);

    // The 2-line truncation itself is CSS (-webkit-line-clamp in task-card.css),
    // which jsdom does not compute; assert the clamped element renders its text.
    const description = element.querySelector('.task-description');
    expect(description?.textContent).toContain('recorta a dos líneas');

    expect(element.textContent).toContain('Escribir README');
    expect(button(element, 'Editar')).toBeTruthy();
    expect(button(element, 'Eliminar')).toBeTruthy();
  });

  it('maps MEDIUM and LOW priorities to Media and Baja', () => {
    const fixture = TestBed.createComponent(TaskCard);
    fixture.componentRef.setInput('task', makeTask('MEDIUM', null));
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.badge')?.textContent?.trim()).toBe(
      'Media',
    );

    fixture.componentRef.setInput('task', makeTask('LOW', null));
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.badge')?.textContent?.trim()).toBe(
      'Baja',
    );
  });
});
