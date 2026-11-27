import {
  HttpContext,
  HttpContextToken,
  HttpErrorResponse,
  HttpInterceptorFn,
} from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';
import { isProblemDetail, type ProblemDetail } from './problem-detail';

const SNACKBAR_DURATION_MS = 5000;

/**
 * Set on a request to handle its errors yourself (e.g. the detail page shows a "not found" view for a
 * 404 instead of a snackbar). The error is still rethrown either way.
 */
export const SKIP_ERROR_SNACKBAR = new HttpContextToken<boolean>(() => false);

/**
 * Shortcut for the opt-out, e.g. with `httpResource`:
 * `httpResource<EventDetail>(() => ({ url: eventUrl(id()), context: skipErrorSnackbar() }))`
 */
export function skipErrorSnackbar(): HttpContext {
  return new HttpContext().set(SKIP_ERROR_SNACKBAR, true);
}

/**
 * Shows a snackbar for every failed HTTP request, using the backend's ProblemDetail when there is one,
 * then rethrows so the caller (store, `httpResource`) still sees the error and can show its own state.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notifier = inject(ErrorNotifier);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && !req.context.get(SKIP_ERROR_SNACKBAR)) {
        notifier.show(errorMessage(error));
      }
      return throwError(() => error);
    }),
  );
};

/**
 * Opens the error snackbar. MatSnackBar shows one message at a time and replaces the current one, so
 * when the backend is down and several requests fail at once, the same "Can't reach the server" would
 * flash once per request. While a message is on screen, the same message is not opened again.
 */
@Injectable({ providedIn: 'root' })
export class ErrorNotifier {
  private readonly snackBar = inject(MatSnackBar);
  private showing: string | null = null;

  show(message: string): void {
    if (message === this.showing) return;
    this.showing = message;

    this.snackBar
      .open(message, 'Close', {
        duration: SNACKBAR_DURATION_MS,
        // errors interrupt screen readers instead of waiting for them to finish (default is 'polite')
        politeness: 'assertive',
      })
      .afterDismissed()
      .subscribe(() => {
        // only reset if a newer, different message hasn't replaced this one
        if (this.showing === message) this.showing = null;
      });
  }
}

/** The text shown to the user for a failed request. Exported for tests. */
export function errorMessage(error: HttpErrorResponse): string {
  // no response at all: backend down, network gone, CORS, request blocked
  if (error.status === 0) {
    return "Can't reach the server. Check your connection and try again.";
  }

  // from our backend: GlobalExceptionHandler (P1-07)
  if (isProblemDetail(error.error)) {
    return problemMessage(error.error);
  }

  // anything else: an HTML error page, a proxy error, a non-JSON body
  return `Something went wrong (HTTP ${error.status}). Please try again.`;
}

function problemMessage(problem: ProblemDetail): string {
  const [first, ...rest] = problem.errors ?? [];
  if (first) {
    // "Validation Failed: size must be less than or equal to 50 (+1 more)"
    const more = rest.length > 0 ? ` (+${rest.length} more)` : '';
    return `${problem.title ?? 'Invalid request'}: ${first.field} ${first.message}${more}`;
  }

  // our backend's `detail` is always safe to show (500s get a generic sentence, never the exception message)
  return problem.detail ?? problem.title ?? `Something went wrong (HTTP ${problem.status}).`;
}
