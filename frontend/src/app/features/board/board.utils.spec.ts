/** TEST-018 / TEST-019 — pure `moveTask` (SPEC §12.2, board R6). No TestBed. */
import { type Task, type TaskStatus } from '../../core/models/task.model';
import { moveTask } from './board.utils';

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

/** Reads one column as [id, position] pairs, sorted by position. */
const column = (tasks: Task[], status: TaskStatus): [number, number][] =>
  tasks
    .filter((task) => task.status === status)
    .sort((a, b) => a.position - b.position)
    .map((task) => [task.id, task.position]);

describe('moveTask', () => {
  it('TEST-018: moves a task across columns, renumbering both, without mutating the input', () => {
    const tasks = [makeTask(1, 'TODO', 0), makeTask(2, 'TODO', 1), makeTask(3, 'TODO', 2)];

    const result = moveTask(tasks, 1, 'IN_PROGRESS', 0);

    expect(column(result, 'IN_PROGRESS')).toEqual([[1, 0]]);
    expect(column(result, 'TODO')).toEqual([
      [2, 0],
      [3, 1],
    ]);

    // The original list is unmodified.
    expect(column(tasks, 'TODO')).toEqual([
      [1, 0],
      [2, 1],
      [3, 2],
    ]);
    expect(column(tasks, 'IN_PROGRESS')).toEqual([]);
  });

  it('TEST-019: reorders within the same column', () => {
    const tasks = [makeTask(1, 'TODO', 0), makeTask(2, 'TODO', 1), makeTask(3, 'TODO', 2)];

    const result = moveTask(tasks, 1, 'TODO', 2);

    expect(column(result, 'TODO')).toEqual([
      [2, 0],
      [3, 1],
      [1, 2],
    ]);
    // The original list is unmodified.
    expect(column(tasks, 'TODO')).toEqual([
      [1, 0],
      [2, 1],
      [3, 2],
    ]);
  });

  it('clamps an out-of-range target index to the end (SPEC §8.5.1)', () => {
    const tasks = [makeTask(1, 'TODO', 0), makeTask(2, 'TODO', 1)];

    const result = moveTask(tasks, 1, 'DONE', 5);

    expect(column(result, 'DONE')).toEqual([[1, 0]]);
    expect(column(result, 'TODO')).toEqual([[2, 0]]);
  });

  it('keeps the list untouched for an unknown task id', () => {
    const tasks = [makeTask(1, 'TODO', 0)];

    const result = moveTask(tasks, 99, 'DONE', 0);

    expect(result).toEqual(tasks);
    expect(result).not.toBe(tasks);
  });
});
