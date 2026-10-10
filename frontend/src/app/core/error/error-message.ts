import { HttpErrorResponse } from '@angular/common/http';
import type { ProblemDetail } from '../models/problem-detail.model';

/** Default text for a failed action (SPEC §8.2). */
export const ACTION_ERROR_MESSAGE = 'No se pudo completar la acción. Inténtalo de nuevo.';

/**
 * Converts a failed HTTP response into a Spanish user-facing message (SPEC §8.2,
 * foundation R4). A ProblemDetail validation failure (HTTP 400) is shown literally;
 * every other failure (500, 404, network) gets the default text.
 * Rendering belongs to components (F6: pure function, no interceptor).
 */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status === 400) {
    const body = error.error as Partial<ProblemDetail> | null | undefined;
    if (body !== null && body !== undefined && typeof body.detail === 'string' && body.detail.length > 0) {
      return body.detail;
    }
  }
  return ACTION_ERROR_MESSAGE;
}
