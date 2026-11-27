import { citiesUrl, eventLayoutUrl, eventsUrl, eventUrl } from './catalog-api';

describe('catalog-api', () => {
  const id = '3f2c9e1a-5b7d-4c11-9a0e-2d8f6b4c1e77';

  describe('eventsUrl', () => {
    it('has no params without a filter', () => {
      expect(eventsUrl()).toEqual({ url: '/api/events', params: {} });
    });

    it('passes every filter through', () => {
      expect(
        eventsUrl({
          city: 'Barcelona',
          from: '2026-12-01',
          to: '2026-12-31',
          status: 'ON_SALE',
          page: 1,
          size: 12,
          sort: 'startsAt,asc',
        }),
      ).toEqual({
        url: '/api/events',
        params: {
          city: 'Barcelona',
          from: '2026-12-01',
          to: '2026-12-31',
          status: 'ON_SALE',
          page: 1,
          size: 12,
          sort: 'startsAt,asc',
        },
      });
    });

    it('drops undefined and blank values and trims strings', () => {
      expect(eventsUrl({ city: '   ', from: undefined, sort: ' title,desc ' }).params).toEqual({
        sort: 'title,desc',
      });
    });

    it('keeps page 0 (falsy but meaningful)', () => {
      expect(eventsUrl({ page: 0 }).params).toEqual({ page: 0 });
    });
  });

  it('builds the event detail url', () => {
    expect(eventUrl(id)).toBe(`/api/events/${id}`);
  });

  it('builds the layout url', () => {
    expect(eventLayoutUrl(id)).toBe(`/api/events/${id}/layout`);
  });

  it('encodes the id so it cannot change the path', () => {
    expect(eventUrl('a/b?c')).toBe('/api/events/a%2Fb%3Fc');
  });

  it('builds the cities url', () => {
    expect(citiesUrl()).toBe('/api/cities');
  });
});
