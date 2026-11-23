package dev.dropline.catalog.domain.layout;

import java.util.List;
import java.util.stream.IntStream;

public class LayoutWalker {

    public int countSeats(LayoutNode node) {
        return switch (node) {
            case SeatBlock seatBlock ->
                seatBlock.rows().stream().mapToInt(SeatBlock.Row::seatCount).sum();

            case Section section ->
                section.children().stream().mapToInt(this::countSeats).sum();
        };
    }

    public List<SeatPosition> flatten(LayoutNode layout) {
        return flattenRec(layout, "");
    }

    private List<SeatPosition> flattenRec(LayoutNode node, String path) {
        return switch (node) {
            case SeatBlock seatBlock -> {
                String resPath = path.isEmpty() ? seatBlock.name() : path + "/" + seatBlock.name();

                yield seatBlock.rows().stream()
                        .flatMap(row -> IntStream.rangeClosed(1, row.seatCount())
                                .mapToObj(idx -> new SeatPosition(resPath, row.label(), idx)))
                        .toList();
            }

            case Section section -> {
                String resPath = path.isEmpty() ? section.name() : path + "/" + section.name();

                yield section.children().stream()
                        .flatMap(child -> flattenRec(child, resPath).stream())
                        .toList();
            }
        };
    }

    public void validateDepth(LayoutNode node) {
        validateDepthRec(node, 0);
    }

    private void validateDepthRec(LayoutNode node, int levels) {
        levels++;

        if (levels > 5) {
            throw new IllegalArgumentException("Layout cannot exceed 5 levels");
        }

        switch (node) {
            case SeatBlock ignored -> {
                // Leaf node
            }

            case Section section -> {
                for (LayoutNode child : section.children()) {
                    validateDepthRec(child, levels);
                }
            }
        }
    }
}
