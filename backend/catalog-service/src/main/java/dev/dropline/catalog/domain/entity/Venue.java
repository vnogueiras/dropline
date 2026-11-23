package dev.dropline.catalog.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tools.jackson.databind.JsonNode;

@Entity
@Table(name = "venue")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Venue {

    @Id
    private UUID id;

    @NotBlank
    @Size(max = 120)
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @NotBlank
    @Size(max = 80)
    @Column(name = "city", nullable = false, length = 80)
    private String city;

    @NotBlank
    @Size(max = 40)
    @Column(name = "timezone", nullable = false, length = 40)
    private String timezone;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout", nullable = false)
    private JsonNode layout;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Venue(String name, String city, String timezone, JsonNode layout) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Venue name cannot be blank");
        }
        if (name.length() > 120) {
            throw new IllegalArgumentException("Venue name cannot exceed 120 characters");
        }

        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException("City name cannot be blank");
        }
        if (city.length() > 80) {
            throw new IllegalArgumentException("City cannot exceed 80 characters");
        }

        if (timezone == null || timezone.isBlank()) {
            throw new IllegalArgumentException("Timezone cannot be blank");
        }
        if (timezone.length() > 40) {
            throw new IllegalArgumentException("Timezone cannot exceed 40 characters");
        }

        if (layout == null) {
            throw new IllegalArgumentException("Layout cannot be null");
        }

        try {
            ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("Invalid timezone: " + timezone, e);
        }

        this.id = UUID.randomUUID();
        this.name = name;
        this.city = city;
        this.timezone = timezone;
        this.layout = layout.deepCopy();
        this.createdAt = Instant.now();
    }
}
