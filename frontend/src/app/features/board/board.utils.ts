import { TASK_STATUSES, type Task, type TaskStatus } from '../../core/models/task.model';

/**
 * Pure move of a task across/within board columns (SPEC §8.5.1) — the client
 * mirror of the backend semantics of SPEC §7.4:
 *
 * - never mutates the input list or its task objects;
 * - the task is inserted at `targetIndex` of the destination column computed
 *   WITHOUT the task itself (same rules as the server);
 * - `targetIndex` beyond the column size is clamped to the end (the CDK never
 *   emits it, but the function must not throw);
 * - both affected columns are renumbered to the contiguous sequence 0..n-1.
 */
export function moveTask(
  tasks: readonly Task[],
  taskId: number,
  targetStatus: TaskStatus,
  targetIndex: number,
): Task[] {
  const moving = tasks.find((task) => task.id === taskId);
  if (moving === undefined) {
    return [...tasks];
  }

  const sourceStatus = moving.status;
  const others = tasks.filter((task) => task.id !== taskId);
  const byPosition = (a: Task, b: Task): number => a.position - b.position;
  const destination = others.filter((task) => task.status === targetStatus).sort(byPosition);
  const insertAt = Math.min(Math.max(targetIndex, 0), destination.length);

  const result: Task[] = [];
  for (const status of TASK_STATUSES) {
    if (status === targetStatus) {
      // Destination column: the moved task is inserted first, then renumbered.
      destination.splice(insertAt, 0, { ...moving, status: targetStatus });
      destination.forEach((task, index) => result.push({ ...task, position: index }));
    } else if (status === sourceStatus) {
      // Source column (only when it differs): renumber without the moved task.
      others
        .filter((task) => task.status === status)
        .sort(byPosition)
        .forEach((task, index) => result.push({ ...task, position: index }));
    } else {
      // Unaffected column: keep tasks (and positions) as they are.
      result.push(...others.filter((task) => task.status === status));
    }
  }
  return result;
}
