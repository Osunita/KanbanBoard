/** Delta scenario — Spanish error mapping: validation detail verbatim, default otherwise (foundation R4). */
import { HttpErrorResponse } from '@angular/common/http';
import { ACTION_ERROR_MESSAGE, errorMessage, isNotFound } from './error-message';

describe('errorMessage', () => {
  it('returns a ProblemDetail validation detail verbatim', () => {
    const error = new HttpErrorResponse({
      error: {
        type: 'about:blank',
        title: 'Bad Request',
        status: 400,
        detail: 'Validation failed',
        instance: '/api/projects',
        errors: [{ field: 'name', message: 'must not be blank' }],
      },
      status: 400,
      statusText: 'Bad Request',
      url: '/api/projects',
    });

    expect(errorMessage(error)).toBe('Validation failed');
  });

  it('falls back to the default text without a usable detail or on server failures', () => {
    const withoutDetail = new HttpErrorResponse({
      error: null,
      status: 400,
      statusText: 'Bad Request',
    });
    const serverError = new HttpErrorResponse({
      error: 'boom',
      status: 500,
      statusText: 'Internal Server Error',
    });

    expect(errorMessage(withoutDetail)).toBe(ACTION_ERROR_MESSAGE);
    expect(errorMessage(serverError)).toBe(ACTION_ERROR_MESSAGE);
  });
});

describe('isNotFound', () => {
  it('detects 404 responses only', () => {
    const notFound = new HttpErrorResponse({ status: 404, statusText: 'Not Found' });
    const serverError = new HttpErrorResponse({ status: 500, statusText: 'Internal Server Error' });

    expect(isNotFound(notFound)).toBe(true);
    expect(isNotFound(serverError)).toBe(false);
    expect(isNotFound('boom')).toBe(false);
  });
});
