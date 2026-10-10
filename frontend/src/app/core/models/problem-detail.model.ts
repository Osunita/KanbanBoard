/**
 * RFC 9457 (ProblemDetail) error body returned by the backend (SPEC §7.5).
 */
export interface FieldError {
  field: string;
  message: string;
}

export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  /** Present only on body-validation failures. */
  errors?: FieldError[];
}
