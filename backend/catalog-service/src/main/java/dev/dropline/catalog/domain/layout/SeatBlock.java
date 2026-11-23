package dev.dropline.catalog.domain.layout;

import java.util.List;
import java.util.Objects;

public record SeatBlock(String name, List<Row> rows) implements LayoutNode {

    public SeatBlock {
        Objects.requireNonNull(name, "Seat block name cannot be null");
        Objects.requireNonNull(rows, "Seat block rows cannot be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException("Seat block name cannot be blank");
        }

        rows = List.copyOf(rows);
    }

    public record Row(String label, int seatCount) {
        public Row {
            Objects.requireNonNull(label, "Row label cannot be null");

            if (label.isBlank()) {
                throw new IllegalArgumentException("Row label cannot be blank");
            }

            if (seatCount <= 0) {
                throw new IllegalArgumentException("Seat count must be positive");
            }
        }
    }
}
