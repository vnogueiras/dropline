package dev.dropline.catalog.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @Id
    private UUID id;

    @Size(max = 200)
    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "starts_at")
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "sale_starts_at")
    private Instant saleStartsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private EventStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Event(
            Venue venue,
            String title,
            String description,
            String imageUrl,
            Instant startsAt,
            Instant endsAt,
            Instant saleStartsAt) {

        if (venue == null) {
            throw new IllegalArgumentException("Venue cannot be null");
        }

        if (title != null && title.length() > 200) {
            throw new IllegalArgumentException("Title cannot exceed 200 characters");
        }

        if (startsAt != null && endsAt != null && !endsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("Event end must be after event start");
        }

        if (saleStartsAt != null && startsAt != null && saleStartsAt.isAfter(startsAt)) {
            throw new IllegalArgumentException("Sale cannot start after the event starts");
        }

        this.id = UUID.randomUUID();
        this.venue = venue;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.saleStartsAt = saleStartsAt;
        this.status = EventStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = null;
    }
}
