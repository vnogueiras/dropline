import {
  HttpClient,
  HttpErrorResponse,
  type HttpContext,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject } from 'rxjs';
import { errorInterceptor, errorMessage, skipErrorSnackbar } from './error.interceptor';
import type { ProblemDetail } from './problem-detail';

const URL = '/api/events/3f2c9e1a';

const notFound: ProblemDetail = {
  type: 'https://dropline.dev/problems/event-not-found',
  title: 'Event Not Found',
  status: 404,
  detail: 'Event 3f2c9e1a was not found',
  resource: 'Event',
  resourceId: '3f2c9e1a',
};

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;
  let dismissed: Subject<void>;
  let open: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    dismissed = new Subject<void>();
    open = vi.fn(() => ({ afterDismissed: () => dismissed.asObservable() }));

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: MatSnackBar, useValue: { open } },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpTesting.verify());

  /** Sends a GET and records what reaches the caller. */
  function request(context?: HttpContext) {
    const result: { error?: unknown; value?: unknown } = {};
    http.get(URL, { context }).subscribe({
      next: (value) => (result.value = value),
      error: (error: unknown) => (result.error = error),
    });
    return result;
  }

  function failWithProblem(problem: ProblemDetail) {
    httpTesting.expectOne(URL).flush(problem, {
      status: problem.status,
      statusText: 'Error',
      headers: { 'Content-Type': 'application/problem+json' },
    });
  }

  it('shows the problem detail in a snackbar', () => {
    request();
    failWithProblem(notFound);

    expect(open).toHaveBeenCalledOnce();
    expect(open).toHaveBeenCalledWith(
      'Event 3f2c9e1a was not found',
      'Close',
      expect.objectContaining({ duration: 5000, politeness: 'assertive' }),
    );
  });

  it('still passes the error to the caller (rethrow)', () => {
    const result = request();
    failWithProblem(notFound);

    expect(result.error).toBeInstanceOf(HttpErrorResponse);
    expect((result.error as HttpErrorResponse).status).toBe(404);
    expect(result.value).toBeUndefined();
  });

  it('does not open a snackbar when the request opts out, but still rethrows', () => {
    const result = request(skipErrorSnackbar());
    failWithProblem(notFound);

    expect(open).not.toHaveBeenCalled();
    expect(result.error).toBeInstanceOf(HttpErrorResponse);
  });

  it('shows a connection message when the server cannot be reached', () => {
    request();
    httpTesting.expectOne(URL).error(new ProgressEvent('error'));

    expect(open).toHaveBeenCalledWith(
      "Can't reach the server. Check your connection and try again.",
      'Close',
      expect.anything(),
    );
  });

  it('does nothing for successful responses', () => {
    const result = request();
    httpTesting.expectOne(URL).flush({ id: '3f2c9e1a' });

    expect(open).not.toHaveBeenCalled();
    expect(result.value).toEqual({ id: '3f2c9e1a' });
  });

  it('does not repeat the same message while it is still on screen', () => {
    http.get('/api/a').subscribe({ error: () => undefined });
    http.get('/api/b').subscribe({ error: () => undefined });
    httpTesting.expectOne('/api/a').error(new ProgressEvent('error'));
    httpTesting.expectOne('/api/b').error(new ProgressEvent('error'));

    expect(open).toHaveBeenCalledOnce();
  });

  it('shows the same message again after the previous one was dismissed', () => {
    request();
    httpTesting.expectOne(URL).error(new ProgressEvent('error'));
    dismissed.next();

    request();
    httpTesting.expectOne(URL).error(new ProgressEvent('error'));

    expect(open).toHaveBeenCalledTimes(2);
  });

  it('shows a different message even while another one is on screen', () => {
    request();
    httpTesting.expectOne(URL).error(new ProgressEvent('error'));
    request();
    failWithProblem(notFound);

    expect(open).toHaveBeenCalledTimes(2);
  });
});

describe('errorMessage', () => {
  const response = (status: number, error: unknown) =>
    new HttpErrorResponse({ status, error, url: URL });

  it('uses the detail of a problem', () => {
    expect(errorMessage(response(404, notFound))).toBe('Event 3f2c9e1a was not found');
  });

  it('falls back to the title when there is no detail', () => {
    expect(errorMessage(response(409, { status: 409, title: 'Conflict' }))).toBe('Conflict');
  });

  it('shows the first validation error and how many more there are', () => {
    const problem: ProblemDetail = {
      title: 'Validation Failed',
      status: 400,
      detail: 'The request has 2 invalid value(s)',
      errors: [
        { field: 'page', message: 'must be greater than or equal to 0' },
        { field: 'size', message: 'must be less than or equal to 50' },
      ],
    };
    expect(errorMessage(response(400, problem))).toBe(
      'Validation Failed: page must be greater than or equal to 0 (+1 more)',
    );
  });

  it('shows a single validation error without a count', () => {
    const problem: ProblemDetail = {
      title: 'Validation Failed',
      status: 400,
      errors: [{ field: 'size', message: 'must be less than or equal to 50' }],
    };
    expect(errorMessage(response(400, problem))).toBe(
      'Validation Failed: size must be less than or equal to 50',
    );
  });

  it('uses a generic message for a body that is not a problem', () => {
    expect(errorMessage(response(502, '<html>Bad Gateway</html>'))).toBe(
      'Something went wrong (HTTP 502). Please try again.',
    );
  });

  it('uses the connection message for status 0', () => {
    expect(errorMessage(response(0, new ProgressEvent('error')))).toBe(
      "Can't reach the server. Check your connection and try again.",
    );
  });
});
