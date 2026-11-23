package dev.dropline.catalog.domain.layout;

import java.util.List;

public record Section(String name, List<LayoutNode> children) implements LayoutNode {}
