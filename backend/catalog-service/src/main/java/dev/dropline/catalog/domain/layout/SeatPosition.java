package dev.dropline.catalog.domain.layout;

import java.util.Objects;

public record SeatPosition(String sectionPath, String rowLabel, int seatNumber) {

    public SeatPosition {
        Objects.requireNonNull(sectionPath, "Section path cannot be null");
        Objects.requireNonNull(rowLabel, "Row label cannot be null");

        if (sectionPath.isBlank()) {
            throw new IllegalArgumentException("Section path cannot be blank");
        }

        if (rowLabel.isBlank()) {
            throw new IllegalArgumentException("Row label cannot be blank");
        }

        if (seatNumber <= 0) {
            throw new IllegalArgumentException("Seat number must be positive");
        }
    }
}
