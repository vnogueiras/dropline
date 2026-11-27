/**
 * RFC 9457 error body (`Content-Type: application/problem+json`) as sent by the catalog service.
 *
 * Written by hand because the OpenAPI spec doesn't document error responses yet (P1-07 step 10, tech debt).
 * Mirrors `GlobalExceptionHandler` / `ProblemMapper` in catalog-service: keep them in sync.
 */
export interface ProblemDetail {
  /** URI identifying the problem type, e.g. `https://dropline.dev/problems/event-not-found`. */
  type?: string;
  /** Short, human-readable summary, e.g. `Event Not Found`. */
  title?: string;
  /** HTTP status code, repeated in the body. */
  status: number;
  /** Explanation of this occurrence, e.g. `Event 3f2c… was not found`. Generic for 500s. */
  detail?: string;
  /** Request path that caused the problem, e.g. `/api/events/3f2c…`. */
  instance?: string;

  // extension members, present only for some problem types
  /** `validation-failed`: one entry per invalid field or parameter, sorted by field. */
  errors?: ValidationError[];
  /** `*-not-found`: which kind of resource was missing, e.g. `Event`. */
  resource?: string;
  /** `*-not-found`: the id that was looked up. */
  resourceId?: string;
}

export interface ValidationError {
  field: string;
  message: string;
}

/**
 * Checks that an unknown value (e.g. `HttpErrorResponse.error`) looks like a ProblemDetail.
 *
 * Only `status` is required by our backend's shape; everything else is checked when present, so a
 * malformed body (an HTML error page, a proxy error string) is rejected instead of being trusted.
 */
export function isProblemDetail(body: unknown): body is ProblemDetail {
  if (typeof body !== 'object' || body === null) return false;
  const problem = body as Record<string, unknown>;

  return (
    typeof problem['status'] === 'number' &&
    optional(problem['type'], 'string') &&
    optional(problem['title'], 'string') &&
    optional(problem['detail'], 'string') &&
    optional(problem['instance'], 'string') &&
    (problem['errors'] === undefined ||
      (Array.isArray(problem['errors']) && problem['errors'].every(isValidationError)))
  );
}

function isValidationError(value: unknown): value is ValidationError {
  if (typeof value !== 'object' || value === null) return false;
  const error = value as Record<string, unknown>;
  return typeof error['field'] === 'string' && typeof error['message'] === 'string';
}

function optional(value: unknown, type: 'string' | 'number'): boolean {
  return value === undefined || typeof value === type;
}
