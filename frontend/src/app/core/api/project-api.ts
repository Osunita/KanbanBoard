import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import type { Observable } from 'rxjs';
import type { Project, ProjectRequest } from '../models/project.model';

/**
 * Thin wrapper with one function per project endpoint (SPEC §4.3).
 * This is the only layer allowed to use `HttpClient`.
 */
@Injectable({ providedIn: 'root' })
export class ProjectApi {
  private readonly http = inject(HttpClient);

  list(): Observable<Project[]> {
    return this.http.get<Project[]>('/api/projects');
  }

  get(id: number): Observable<Project> {
    return this.http.get<Project>(`/api/projects/${id}`);
  }

  create(request: ProjectRequest): Observable<Project> {
    return this.http.post<Project>('/api/projects', request);
  }

  update(id: number, request: ProjectRequest): Observable<Project> {
    return this.http.put<Project>(`/api/projects/${id}`, request);
  }

  remove(id: number): Observable<void> {
    return this.http.delete<void>(`/api/projects/${id}`);
  }
}
