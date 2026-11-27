package dev.dropline.catalog.api.dto;

import dev.dropline.catalog.domain.layout.LayoutNode;
import java.util.List;
import java.util.UUID;

public record EventLayoutDto(UUID eventId, String venueName, LayoutNode layout, List<SeatDto> seats) {
    public record SeatDto(UUID id, String sectionPath, String rowLabel, int seatNumber, UUID tierId) {}
}
