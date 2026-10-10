/** TEST-023 — optimistic move, rollback and same-position guard (SPEC §12.2, board R8, E-09/E-19). */
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import type { Task, TaskStatus } from '../../core/models/task.model';
import { BoardStore } from './board.store';

const makeTask = (id: number, status: TaskStatus, position: number): Task => ({
  id,
  projectId: 1,
  title: `Task ${id}`,
  description: null,
  status,
  priority: 'MEDIUM',
  position,
  createdAt: '2026-10-10T09:00:00.000Z',
  updatedAt: '2026-10-10T09:00:00.000Z',
});

/** Board shape as [status, task ids] in column order — easy to compare. */
const layout = (store: BoardStore): [TaskStatus, number[]][] =>
  store.columns().map((column) => [column.status, column.tasks.map((task) => task.id)]);

describe('BoardStore', () => {
  let store: BoardStore;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    store = TestBed.inject(BoardStore);
  });

  afterEach(() => httpMock.verify());

  const loadBoard = (): Task[] => {
    const tasks = [makeTask(1, 'TODO', 0), makeTask(2, 'TODO', 1), makeTask(3, 'TODO', 2)];
    store.load(1);
    httpMock.expectOne('/api/projects/1/tasks').flush(tasks);
    return tasks;
  };

  it('TEST-023: applies the move optimistically, then restores the snapshot and alerts on 500', () => {
    const original = loadBoard();

    store.move(1, 'IN_PROGRESS', 0);

    // Optimistic state observed while the PATCH is still in flight.
    expect(layout(store)).toEqual([
      ['TODO', [2, 3]],
      ['IN_PROGRESS', [1]],
      ['DONE', []],
    ]);

    const patch = httpMock.expectOne('/api/tasks/1/move');
    expect(patch.request.body).toEqual({ status: 'IN_PROGRESS', position: 0 });
    patch.flush(null, { status: 500, statusText: 'Internal Server Error' });

    // Rollback: the task is back at its original position…
    expect(store.tasks()).toEqual(original);
    expect(layout(store)).toEqual([
      ['TODO', [1, 2, 3]],
      ['IN_PROGRESS', []],
      ['DONE', []],
    ]);
    // …and the dismissible alert carries the default action error (SPEC §8.2).
    expect(store.actionError()).toBe('No se pudo completar la acción. Inténtalo de nuevo.');
  });

  it('sends no request and keeps the state when origin status and index match (E-09)', () => {
    loadBoard();
    const before = store.tasks();

    store.move(2, 'TODO', 1); // Task 2 already sits at TODO index 1

    httpMock.expectNone(() => true);
    expect(store.tasks()).toEqual(before);
    expect(store.actionError()).toBeNull();
  });

  it('reconciles silently after a successful move without toggling loading', () => {
    loadBoard();

    store.move(1, 'DONE', 0);

    const patch = httpMock.expectOne('/api/tasks/1/move');
    expect(patch.request.body).toEqual({ status: 'DONE', position: 0 });
    patch.flush(makeTask(1, 'DONE', 0));

    // Silent reconcile (SPEC §8.5): no spinner while the GET is in flight.
    expect(store.loading()).toBe(false);
    expect(store.actionError()).toBeNull();

    httpMock
      .expectOne('/api/projects/1/tasks')
      .flush([makeTask(2, 'TODO', 0), makeTask(3, 'TODO', 1), makeTask(1, 'DONE', 0)]);

    expect(layout(store)).toEqual([
      ['TODO', [2, 3]],
      ['IN_PROGRESS', []],
      ['DONE', [1]],
    ]);
  });

  it('creates, edits and deletes tasks through the API', () => {
    loadBoard();

    store.create({ title: 'Nueva' });
    httpMock
      .expectOne('/api/projects/1/tasks')
      .flush(makeTask(4, 'TODO', 3), { status: 201, statusText: 'Created' });
    expect(layout(store)).toEqual([
      ['TODO', [1, 2, 3, 4]],
      ['IN_PROGRESS', []],
      ['DONE', []],
    ]);

    store.edit(4, { title: 'Editada', priority: 'HIGH' });
    httpMock.expectOne('/api/tasks/4').flush({ ...makeTask(4, 'TODO', 3), title: 'Editada', priority: 'HIGH' });
    expect(store.tasks().find((task) => task.id === 4)?.title).toBe('Editada');

    store.delete(2);
    httpMock.expectOne('/api/tasks/2').flush(null, { status: 204, statusText: 'No Content' });
    // Source column renumbered 0..n-1 locally.
    expect(layout(store)).toEqual([
      ['TODO', [1, 3, 4]],
      ['IN_PROGRESS', []],
      ['DONE', []],
    ]);
    expect(
      store.columns()[0].tasks.map((task) => task.position),
    ).toEqual([0, 1, 2]);
  });

  it('dismisses the action error notice on demand (SPEC §8.4)', () => {
    loadBoard();

    store.move(1, 'IN_PROGRESS', 0);
    httpMock
      .expectOne('/api/tasks/1/move')
      .flush(null, { status: 500, statusText: 'Internal Server Error' });
    expect(store.actionError()).toBe('No se pudo completar la acción. Inténtalo de nuevo.');

    store.dismissActionError();
    expect(store.actionError()).toBeNull();
  });
});
