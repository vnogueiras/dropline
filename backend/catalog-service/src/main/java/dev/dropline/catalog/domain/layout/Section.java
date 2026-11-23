package dev.dropline.catalog.domain.layout;

import java.util.List;
import java.util.Objects;

public record Section(String name, List<LayoutNode> children) implements LayoutNode {

    public Section {
        Objects.requireNonNull(name, "Section name cannot be null");
        Objects.requireNonNull(children, "Section children cannot be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException("Section name cannot be blank");
        }

        children = List.copyOf(children);
    }
}
