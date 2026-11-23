package dev.dropline.catalog.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
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

    @Column(name = "price_cents")
    private int priceCents;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency", length = 3)
    private String currency;

    public PriceTier(Event event, String name, int priceCents, String currency) {
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (name != null && name.length() > 40) {
            throw new IllegalArgumentException("Name cannot exceed 40 characters");
        }
        if (priceCents < 0) {
            throw new IllegalArgumentException("Price cannot be < 0");
        }
        if (currency != null && currency.length() != 3) {
            throw new IllegalArgumentException("Currency is badly formatted");
        }
        if (currency == null) {
            currency = "EUR";
        }
        currency = currency.toUpperCase(Locale.ROOT);
        for (char c : currency.toCharArray()) {
            if (c < 'A' || c > 'Z') {
                throw new IllegalArgumentException("Currency must be in alphabetical characters");
            }
        }

        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        this.id = UUID.randomUUID();
        this.event = event;
        this.name = name;
        this.priceCents = priceCents;
        this.currency = currency;
    }
}
