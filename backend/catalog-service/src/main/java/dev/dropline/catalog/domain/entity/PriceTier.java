package dev.dropline.catalog.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "price_tier")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceTier {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Size(max = 40)
    @Column(name = "name", length = 40)
    private String name;

    @Column(name = "price_cents", nullable = false)
    private int priceCents;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    public PriceTier(Event event, String name, int priceCents, String currency) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be blank");
        }

        if (name != null && name.length() > 40) {
            throw new IllegalArgumentException("Name cannot exceed 40 characters");
        }

        if (priceCents < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }

        currency = currency == null ? "EUR" : currency.toUpperCase(Locale.ROOT);

        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid currency: " + currency, e);
        }

        this.id = UUID.randomUUID();
        this.event = event;
        this.name = name;
        this.priceCents = priceCents;
        this.currency = currency;
    }
}
