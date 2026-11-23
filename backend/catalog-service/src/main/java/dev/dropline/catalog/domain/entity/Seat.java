package dev.dropline.catalog.domain.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "seat",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"event_id", "section_path", "row_label", "seat_number"})})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id")
    private PriceTier tier;

    @Size(max = 200)
    @Column(name = "section_path", nullable = false, length = 200)
    private String sectionPath;

    @Size(max = 4)
    @Column(name = "row_label", nullable = false, length = 4)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    public Seat(Event event, String sectionPath, String rowLabel, int seatNumber, PriceTier tier) {

        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        if (sectionPath == null || sectionPath.isBlank()) {
            throw new IllegalArgumentException("Section path cannot be blank");
        }

        if (sectionPath.length() > 200) {
            throw new IllegalArgumentException("Section path cannot exceed 200 characters");
        }

        if (rowLabel == null || rowLabel.isBlank()) {
            throw new IllegalArgumentException("Row label cannot be blank");
        }

        if (rowLabel.length() > 4) {
            throw new IllegalArgumentException("Row label cannot exceed 4 characters");
        }

        if (seatNumber <= 0) {
            throw new IllegalArgumentException("Seat number must be positive");
        }

        if (tier != null && !tier.getEvent().getId().equals(event.getId())) {
            throw new IllegalArgumentException("Price tier must belong to the same event");
        }

        this.id = UUID.randomUUID();
        this.event = event;
        this.sectionPath = sectionPath;
        this.rowLabel = rowLabel;
        this.seatNumber = seatNumber;
        this.tier = tier;
    }
}
