import { isProblemDetail } from './problem-detail';

describe('isProblemDetail', () => {
  it('accepts a not-found problem from the backend', () => {
    expect(
      isProblemDetail({
        type: 'https://dropline.dev/problems/event-not-found',
        title: 'Event Not Found',
        status: 404,
        detail: 'Event 3f2c9e1a was not found',
        instance: '/api/events/3f2c9e1a',
        resource: 'Event',
        resourceId: '3f2c9e1a',
      }),
    ).toBe(true);
  });

  it('accepts a validation problem with errors', () => {
    expect(
      isProblemDetail({
        title: 'Validation Failed',
        status: 400,
        errors: [{ field: 'size', message: 'must be less than or equal to 50' }],
      }),
    ).toBe(true);
  });

  it('accepts the minimal shape (status only)', () => {
    expect(isProblemDetail({ status: 500 })).toBe(true);
  });

  it.each([
    ['null', null],
    ['undefined', undefined],
    ['an HTML error page', '<html>Whitelabel Error Page</html>'],
    ['an object without status', { title: 'Oops' }],
    ['a status that is not a number', { status: '404' }],
    ['a title that is not a string', { status: 404, title: 42 }],
    ['errors that is not an array', { status: 400, errors: 'size' }],
    ['an error entry without a message', { status: 400, errors: [{ field: 'size' }] }],
  ])('rejects %s', (_, body) => {
    expect(isProblemDetail(body)).toBe(false);
  });
});
