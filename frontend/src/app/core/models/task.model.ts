/**
 * Task resource shapes. Mirrors the backend DTO of SPEC §7.2 exactly.
 * Union members are declared in enum order (SPEC §6.1): column order for
 * TaskStatus, ranking order for TaskPriority.
 */
export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';

export const TASK_STATUSES: readonly TaskStatus[] = ['TODO', 'IN_PROGRESS', 'DONE'];

export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export const TASK_PRIORITIES: readonly TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH'];

export interface Task {
  id: number;
  projectId: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  position: number;
  createdAt: string;
  updatedAt: string;
}

/** Body accepted by POST /api/projects/{projectId}/tasks (SPEC §7.1). */
export interface TaskCreateRequest {
  title: string;
  description?: string | null;
  priority?: TaskPriority;
}

/** Body accepted by PUT /api/tasks/{id} (replaces these three fields only). */
export interface TaskUpdateRequest {
  title: string;
  description?: string | null;
  priority: TaskPriority;
}

/** Body accepted by PATCH /api/tasks/{id}/move (SPEC §7.4). */
export interface TaskMoveRequest {
  status: TaskStatus;
  position: number;
}
