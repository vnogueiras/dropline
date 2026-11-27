import type { HttpResourceRequest } from '@angular/common/http';
import type { operations } from './catalog.types';

/**
 * URLs for the catalog API. Only URLs and query params live here: fetching happens with
 * `httpResource` in the stores (P1-13), e.g. `httpResource<Page<EventSummary>>(() => eventsUrl(filter()))`.
 *
 * Relative `/api` on purpose: in dev the Angular proxy forwards it to catalog-service (proxy.conf.json),
 * in production the gateway/ingress serves it on the same domain. Move to `environment` if that changes (step 2).
 */
const API = '/api';

/** Query parameters of `GET /api/events`, taken from the OpenAPI spec (changes with `npm run gen:api`). */
export type EventsFilter = NonNullable<operations['getEvents']['parameters']['query']>;

/** Path parameter `{id}` of `GET /api/events/{id}`. */
export type EventId = operations['getEvent']['parameters']['path']['id'];

type QueryParams = Record<string, string | number | boolean>;

/**
 * `GET /api/events` as an `httpResource` request: `{ url, params }` instead of a string, so Angular
 * encodes the query parameters and the URL stays readable.
 */
export function eventsUrl(filter: EventsFilter = {}): HttpResourceRequest {
  return { url: `${API}/events`, params: toParams(filter) };
}

export function eventUrl(id: EventId): string {
  return `${API}/events/${encodeURIComponent(id)}`;
}

export function eventLayoutUrl(id: EventId): string {
  return `${eventUrl(id)}/layout`;
}

export function citiesUrl(): string {
  return `${API}/cities`;
}

/**
 * Drops filters that shouldn't be sent: `undefined`, `null` and blank strings (so an empty search box
 * doesn't become `?city=`). Strings are trimmed.
 */
function toParams(filter: EventsFilter): QueryParams {
  const params: QueryParams = {};
  for (const [key, value] of Object.entries(filter)) {
    if (value === undefined || value === null) continue;
    if (typeof value === 'string') {
      const trimmed = value.trim();
      if (trimmed) params[key] = trimmed;
    } else {
      params[key] = value;
    }
  }
  return params;
}
