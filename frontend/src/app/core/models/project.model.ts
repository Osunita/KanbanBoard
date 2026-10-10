/**
 * Project resource shapes. Mirrors the backend DTO of SPEC §7.2 exactly.
 */
export interface Project {
  id: number;
  name: string;
  description: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Body accepted by POST /api/projects and PUT /api/projects/{id} (SPEC §7.1). */
export interface ProjectRequest {
  name: string;
  description?: string | null;
}
