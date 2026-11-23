package dev.dropline.catalog.domain.layout;

import java.util.List;

public record SeatBlock(String name, List<Row> rows) implements LayoutNode {

    public record Row(String label, int seatCount) {}
}
