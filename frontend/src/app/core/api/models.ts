/**
 * App-facing names for the catalog API types. Import from here, never from `catalog.types.ts`:
 * if a backend DTO is renamed, only this file changes.
 *
 * The aliases are derived from the generated types, so `npm run gen:api` + a backend change still
 * breaks the build wherever a field is used.
 */
import type { components } from './catalog.types';

type Schemas = components['schemas'];

/** A generated schema by name. The name is checked: a typo is a compile error. */
type Schema<K extends keyof Schemas> = Schemas[K];

/**
 * Marks fields that the backend can send as `null`.
 *
 * TEMPORARY: the spec has no `required`/`nullable` info (Java records), so `gen:api` runs with
 * `--properties-required-by-default` and every field comes out as non-null. The fields below really
 * can be null. Remove these patches once the backend documents nullability (P1-07 step 10, tech debt).
 */
type Nullable<T, K extends keyof T> = Omit<T, K> & { [P in K]: T[P] | null };

// ---------- events ----------

export type EventStatus = Schema<'EventSummaryDto'>['status'];

/** Every status, as a runtime value (e.g. for a filter dropdown). Checked against the union below. */
export const EVENT_STATUSES = [
  'DRAFT',
  'PUBLISHED',
  'ON_SALE',
  'SOLD_OUT',
  'FINISHED',
] as const satisfies readonly EventStatus[];

export type EventSummary = Nullable<Schema<'EventSummaryDto'>, 'imageUrl'>;

export type Venue = Schema<'VenueDto'>;

export type PriceTier = Schema<'PriceTierDto'>;

/** Lowest and highest price in cents; both `null` when the event has no price tiers yet. */
export type PriceRange = Nullable<Schema<'PriceRange'>, 'min' | 'max'>;

export type EventDetail = Nullable<
  Omit<Schema<'EventDetailDto'>, 'priceRange'> & { priceRange: PriceRange },
  'description' | 'imageUrl' | 'cheapestTier'
>;

// ---------- seat map ----------

export type Row = Schema<'Row'>;

// The layout tree is a sealed interface on the backend (Section | SeatBlock, Jackson DEDUCTION: no type
// field). springdoc puts the fields under `allOf`, where they stay optional, so they're made required here.
export type SeatBlock = Required<Schema<'SeatBlock'>>;
export type Section = Omit<Required<Schema<'Section'>>, 'children'> & { children: LayoutNode[] };
export type LayoutNode = Section | SeatBlock;

/** There's no type field in the JSON: a node with `children` is a section, one with `rows` a seat block. */
export function isSection(node: LayoutNode): node is Section {
  return 'children' in node;
}

export type Seat = Nullable<Schema<'SeatDto'>, 'tierId'>;

export type EventLayout = Omit<Schema<'EventLayoutDto'>, 'layout' | 'seats'> & {
  layout: LayoutNode;
  seats: Seat[];
};

// ---------- paging ----------

export type PageMetadata = Schema<'PageMetadata'>;

/** Spring Data `PagedModel` (`spring.data.web.pageable.serialization-mode=via-dto`). */
export interface Page<T> {
  content: T[];
  page: PageMetadata;
}

// ---------- compile-time checks (no runtime code) ----------

type Equals<A, B> =
  (<T>() => T extends A ? 1 : 2) extends <T>() => T extends B ? 1 : 2 ? true : false;
type Assert<T extends true> = T;

// `satisfies` above only checks that each entry is valid; this also fails if a status is missing.
// eslint-disable-next-line @typescript-eslint/no-unused-vars
type AllStatusesListed = Assert<Equals<(typeof EVENT_STATUSES)[number], EventStatus>>;

// Our generic Page<T> must match the shape Spring actually sends (fails if PagedModel changes).
// eslint-disable-next-line @typescript-eslint/no-unused-vars
type PageMatchesSpring = Assert<
  Equals<Page<Schema<'EventSummaryDto'>>, Required<Schema<'PagedModelEventSummaryDto'>>>
>;
