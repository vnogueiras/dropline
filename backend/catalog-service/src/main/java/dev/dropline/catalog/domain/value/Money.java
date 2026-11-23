package dev.dropline.catalog.domain.value;

import java.util.Currency;
import java.util.Locale;

public record Money(int cents, String currency) {

    public Money {
        if (cents < 0) {
            throw new IllegalArgumentException("Money amount cannot be negative");
        }

        currency = currency == null ? "EUR" : currency.toUpperCase(Locale.ROOT);

        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid currency: " + currency, e);
        }
    }
}
