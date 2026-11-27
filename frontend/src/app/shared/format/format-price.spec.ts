import { formatPrice } from './format-price';

/** Intl uses non-breaking spaces in some locales; compare with normal spaces. */
const plain = (text: string) => text.replace(/\s/g, ' ');

describe('formatPrice', () => {
  it('formats euros in Dutch', () => {
    expect(plain(formatPrice(4500, 'EUR', 'nl-NL'))).toBe('€ 45,00');
  });

  it('formats the same price for another locale', () => {
    expect(formatPrice(4500, 'EUR', 'en-US')).toBe('€45.00');
  });

  it('keeps the cents', () => {
    expect(plain(formatPrice(1999, 'EUR', 'nl-NL'))).toBe('€ 19,99');
  });

  it('formats zero', () => {
    expect(plain(formatPrice(0, 'EUR', 'nl-NL'))).toBe('€ 0,00');
  });

  it('does not divide a zero-decimal currency by 100', () => {
    expect(formatPrice(4500, 'JPY', 'en-US')).toBe('¥4,500');
  });

  it('returns an empty string when there is no price', () => {
    expect(formatPrice(null, 'EUR', 'nl-NL')).toBe('');
    expect(formatPrice(undefined, 'EUR', 'nl-NL')).toBe('');
  });

  it('falls back to a readable text for an unknown currency', () => {
    expect(formatPrice(4500, 'XX', 'nl-NL')).toBe('45.00 XX');
  });

  it('gives the same result on repeated calls (cached formatter)', () => {
    const first = formatPrice(4500, 'EUR', 'nl-NL');
    expect(formatPrice(4500, 'EUR', 'nl-NL')).toBe(first);
  });

  it('uses the browser language when no locale is given', () => {
    expect(formatPrice(4500, 'EUR')).toBe(formatPrice(4500, 'EUR', navigator.language));
  });
});
