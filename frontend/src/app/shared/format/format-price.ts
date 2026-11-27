/** Used when the browser doesn't report a language (tests, SSR). */
const FALLBACK_LOCALE = 'nl-NL';

/** Creating an Intl.NumberFormat is expensive compared to using one, so keep one per locale + currency. */
const formatters = new Map<string, Intl.NumberFormat>();

/**
 * Formats a price stored in the currency's minor unit (what the API sends as `priceCents`).
 *
 * ```ts
 * formatPrice(4500, 'EUR', 'nl-NL'); // "€ 45,00"
 * formatPrice(4500, 'EUR', 'en-US'); // "€45.00"
 * formatPrice(4500, 'JPY', 'en-US'); // "¥4,500"  (yen has no minor unit)
 * formatPrice(null, 'EUR');          // ""        (no price yet)
 * ```
 *
 * Not every currency has 2 decimals (JPY has 0, KWD has 3), so the amount is divided by
 * 10^(the currency's fraction digits), not always by 100.
 *
 * Note: some locales put a non-breaking space (U+00A0 / U+202F) between symbol and number.
 *
 * @param minorUnits price in cents (or the currency's minor unit); `null`/`undefined` → `''`
 * @param currency ISO 4217 code, e.g. `'EUR'`
 * @param locale BCP 47 tag; defaults to the browser language
 */
export function formatPrice(
  minorUnits: number | null | undefined,
  currency: string,
  locale: string = browserLocale(),
): string {
  if (minorUnits === null || minorUnits === undefined) {
    return '';
  }

  const formatter = currencyFormatter(locale, currency);
  if (!formatter) {
    // unknown currency code: show something readable instead of breaking the whole list
    return `${(minorUnits / 100).toFixed(2)} ${currency}`;
  }

  const fractionDigits = formatter.resolvedOptions().maximumFractionDigits ?? 2;
  return formatter.format(minorUnits / 10 ** fractionDigits);
}

function currencyFormatter(locale: string, currency: string): Intl.NumberFormat | undefined {
  const key = `${locale}|${currency}`;
  let formatter = formatters.get(key);
  if (!formatter) {
    try {
      formatter = new Intl.NumberFormat(locale, { style: 'currency', currency });
    } catch {
      return undefined; // RangeError: invalid currency code
    }
    formatters.set(key, formatter);
  }
  return formatter;
}

function browserLocale(): string {
  return (typeof navigator !== 'undefined' && navigator.language) || FALLBACK_LOCALE;
}
