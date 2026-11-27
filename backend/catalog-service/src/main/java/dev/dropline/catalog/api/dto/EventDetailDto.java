package dev.dropline.catalog.api.dto;

import dev.dropline.catalog.domain.entity.EventStatus;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record EventDetailDto(
        UUID id,
        String title,
        String description,
        String imageUrl,
        Instant startsAt,
        Instant endsAt,
        Instant saleStartsAt,
        EventStatus status,
        VenueDto venue,
        List<PriceTierDto> priceTiers,
        String cheapestTier,
        PriceRange priceRange,
        long totalSeats,
        Map<String, Long> sections) {

    public record VenueDto(UUID id, String name, String city, String timezone) {}

    public record PriceTierDto(UUID id, String name, int priceCents, String currency, long seatCount) {}

    public record PriceRange(Integer min, Integer max) {}
}
