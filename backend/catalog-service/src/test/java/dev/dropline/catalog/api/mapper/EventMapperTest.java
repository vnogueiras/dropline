package dev.dropline.catalog.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import dev.dropline.catalog.api.dto.EventDetailDto;
import dev.dropline.catalog.api.dto.EventSummaryDto;
import dev.dropline.catalog.domain.entity.Event;
import dev.dropline.catalog.domain.entity.PriceTier;
import dev.dropline.catalog.domain.entity.Venue;
import dev.dropline.catalog.persistence.repository.projection.EventSummaryView;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import tools.jackson.databind.ObjectMapper;

class EventMapperTest {

    private EventMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(EventMapper.class);
    }

    @Test
    void shouldMapEventSummary() {
        EventSummaryView view = org.mockito.Mockito.mock(EventSummaryView.class);

        UUID id = UUID.randomUUID();

        org.mockito.Mockito.when(view.getId()).thenReturn(id);
        org.mockito.Mockito.when(view.getTitle()).thenReturn("Concert");
        org.mockito.Mockito.when(view.getImageUrl()).thenReturn("image.jpg");
        org.mockito.Mockito.when(view.getStartsAt()).thenReturn(Instant.parse("2027-01-01T20:00:00Z"));
        org.mockito.Mockito.when(view.getStatus()).thenReturn(null);
        org.mockito.Mockito.when(view.getVenueName()).thenReturn("Ziggo Dome");
        org.mockito.Mockito.when(view.getCity()).thenReturn("Amsterdam");

        EventSummaryDto dto = mapper.toDto(view);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.title()).isEqualTo("Concert");
        assertThat(dto.imageUrl()).isEqualTo("image.jpg");
        assertThat(dto.venueName()).isEqualTo("Ziggo Dome");
        assertThat(dto.city()).isEqualTo("Amsterdam");
    }

    @Test
    void shouldMapEventDetailsAndSeatCounts() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        Venue venue = new Venue("Ziggo Dome", "Amsterdam", "Europe/Amsterdam", objectMapper.readTree("{}"));

        Event event = new Event(
                venue,
                "Concert",
                "Live music",
                "image.jpg",
                Instant.parse("2027-01-01T20:00:00Z"),
                Instant.parse("2027-01-01T23:00:00Z"),
                Instant.parse("2026-12-01T10:00:00Z"));

        PriceTier regular = new PriceTier(event, "Regular", 5000, "EUR");
        PriceTier vip = new PriceTier(event, "VIP", 10000, "EUR");

        event.getPriceTiers().add(regular);
        event.getPriceTiers().add(vip);

        Map<UUID, Long> seatCounts = Map.of(
                regular.getId(), 100L,
                vip.getId(), 20L);

        EventDetailDto.PriceRange priceRange = new EventDetailDto.PriceRange(5000, 10000);
        Map<String, Long> sections = Map.of("Floor", 100L, "VIP", 20L);
        EventDetailDto dto = mapper.toDetailDto(event, seatCounts, "Regular", priceRange, 120, sections);

        assertThat(dto.title()).isEqualTo("Concert");
        assertThat(dto.description()).isEqualTo("Live music");
        assertThat(dto.venue().name()).isEqualTo("Ziggo Dome");
        assertThat(dto.venue().city()).isEqualTo("Amsterdam");
        assertThat(dto.venue().timezone()).isEqualTo("Europe/Amsterdam");

        assertThat(dto.priceTiers()).hasSize(2);

        assertThat(dto.priceTiers())
                .extracting(EventDetailDto.PriceTierDto::name)
                .containsExactly("Regular", "VIP");

        assertThat(dto.priceTiers())
                .extracting(EventDetailDto.PriceTierDto::seatCount)
                .containsExactly(100L, 20L);

        assertThat(dto.cheapestTier()).isEqualTo("Regular");
        assertThat(dto.priceRange()).isEqualTo(priceRange);
        assertThat(dto.totalSeats()).isEqualTo(120);
        assertThat(dto.sections()).isEqualTo(sections);
    }

    @Test
    void shouldDefaultSeatCountToZeroWhenNoSeatsExist() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        Venue venue = new Venue("Ziggo Dome", "Amsterdam", "Europe/Amsterdam", objectMapper.readTree("{}"));

        Event event = new Event(
                venue,
                "Concert",
                "Live music",
                "image.jpg",
                Instant.parse("2027-01-01T20:00:00Z"),
                Instant.parse("2027-01-01T23:00:00Z"),
                Instant.parse("2026-12-01T10:00:00Z"));

        PriceTier tier = new PriceTier(event, "Regular", 5000, "EUR");
        event.getPriceTiers().add(tier);

        EventDetailDto.PriceRange priceRange = new EventDetailDto.PriceRange(5000, 5000);
        EventDetailDto dto = mapper.toDetailDto(event, Map.of(), "Regular", priceRange, 0, Map.of());

        assertThat(dto.priceTiers()).hasSize(1);
        assertThat(dto.priceTiers().getFirst().seatCount()).isZero();
    }
}
