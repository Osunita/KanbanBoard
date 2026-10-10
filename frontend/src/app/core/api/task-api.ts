import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import type { Observable } from 'rxjs';
import type {
  Task,
  TaskCreateRequest,
  TaskMoveRequest,
  TaskUpdateRequest,
} from '../models/task.model';

/**
 * Thin wrapper with one function per task endpoint (SPEC §4.3).
 * This is the only layer allowed to use `HttpClient`.
 */
@Injectable({ providedIn: 'root' })
export class TaskApi {
  private readonly http = inject(HttpClient);

  list(projectId: number): Observable<Task[]> {
    return this.http.get<Task[]>(`/api/projects/${projectId}/tasks`);
  }

  create(projectId: number, request: TaskCreateRequest): Observable<Task> {
    return this.http.post<Task>(`/api/projects/${projectId}/tasks`, request);
  }

  get(id: number): Observable<Task> {
    return this.http.get<Task>(`/api/tasks/${id}`);
  }

  update(id: number, request: TaskUpdateRequest): Observable<Task> {
    return this.http.put<Task>(`/api/tasks/${id}`, request);
  }

  move(id: number, request: TaskMoveRequest): Observable<Task> {
    return this.http.patch<Task>(`/api/tasks/${id}/move`, request);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`/api/tasks/${id}`);
  }
}
