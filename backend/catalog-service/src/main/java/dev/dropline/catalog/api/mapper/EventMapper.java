package dev.dropline.catalog.api.mapper;

import dev.dropline.catalog.api.dto.EventDetailDto;
import dev.dropline.catalog.api.dto.EventSummaryDto;
import dev.dropline.catalog.domain.entity.Event;
import dev.dropline.catalog.domain.entity.PriceTier;
import dev.dropline.catalog.domain.entity.Venue;
import dev.dropline.catalog.persistence.repository.projection.EventSummaryView;
import java.util.Map;
import java.util.UUID;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    EventSummaryDto toDto(EventSummaryView view);

    @Mapping(target = "venue", source = "event.venue")
    @Mapping(target = "priceTiers", source = "event.priceTiers")
    @Mapping(target = "id", source = "event.id")
    EventDetailDto toDetailDto(
            Event event,
            @Context Map<UUID, Long> seatCounts,
            String cheapestTier,
            EventDetailDto.PriceRange priceRange,
            long totalSeats,
            Map<String, Long> sections);

    EventDetailDto.VenueDto toVenueDto(Venue venue);

    @Mapping(target = "seatCount", expression = "java(seatCounts.getOrDefault(priceTier.getId(), 0L))")
    EventDetailDto.PriceTierDto toPriceTierDto(PriceTier priceTier, @Context Map<UUID, Long> seatCounts);
}
