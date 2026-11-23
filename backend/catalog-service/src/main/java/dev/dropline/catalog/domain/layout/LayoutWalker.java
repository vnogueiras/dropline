package dev.dropline.catalog.domain.layout;

import java.util.List;
import java.util.stream.IntStream;

public class LayoutWalker {

    public int countSeats(LayoutNode node) {
        if (node instanceof SeatBlock seatBlock) {
            return seatBlock.rows().stream().mapToInt(SeatBlock.Row::seatCount).sum();
        }
        if (node instanceof Section section) {
            return section.children().stream().mapToInt(this::countSeats).sum();
        }
        throw new IllegalArgumentException("Unsupported layout node: " + node);
    }

    public List<SeatPosition> flatten(LayoutNode layout) {
        return flattenRec(layout, "");
    }

    private List<SeatPosition> flattenRec(LayoutNode node, String path) {
        if (node instanceof SeatBlock seatBlock) {
            String resPath = path.isEmpty() ? seatBlock.name() : path + "/" + seatBlock.name();
            return seatBlock.rows().stream()
                    .flatMap(row -> IntStream.rangeClosed(1, row.seatCount())
                            .mapToObj(idx -> new SeatPosition(resPath, row.label(), idx)))
                    .toList();
        }
        if (node instanceof Section section) {
            String resPath = path.isEmpty() ? section.name() : path + "/" + section.name();
            return section.children().stream()
                    .flatMap(child -> flattenRec(child, resPath).stream())
                    .toList();
        }
        throw new IllegalArgumentException("Unsupported layout node: " + node);
    }

    public void validateDepth(LayoutNode node) {
        validateDepthRec(node, 0);
    }

    private void validateDepthRec(LayoutNode node, int levels) {
        levels++;
        if (levels > 5) {
            throw new IllegalArgumentException("Layout cannot exceed 5 levels");
        }
        if (node instanceof SeatBlock) {
            return;
        }
        if (node instanceof Section section) {
            for (LayoutNode child : section.children()) {
                validateDepthRec(child, levels);
            }
        }
    }
}
