package dev.dropline.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.dropline.catalog.api.dto.EventDetailDto;
import dev.dropline.catalog.api.mapper.EventMapperImpl;
import dev.dropline.catalog.domain.entity.Event;
import dev.dropline.catalog.domain.entity.PriceTier;
import dev.dropline.catalog.domain.entity.Seat;
import dev.dropline.catalog.domain.entity.Venue;
import dev.dropline.catalog.persistence.repository.EventRepository;
import dev.dropline.catalog.persistence.repository.SeatRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.node.JsonNodeFactory;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({EventService.class, EventMapperImpl.class, EventServiceIT.TestCacheConfiguration.class})
class EventServiceIT {

    @TestConfiguration
    static class TestCacheConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldReturnDetailedEventWithSeatCounts() {
        // Arrange
        Venue venue = new Venue("Ziggo Dome", "Amsterdam", "Europe/Amsterdam", JsonNodeFactory.instance.objectNode());
        entityManager.persist(venue);

        Event event = new Event(
                venue,
                "Test Concert",
                "Live music",
                "image.jpg",
                Instant.parse("2027-06-15T19:00:00Z"),
                Instant.parse("2027-06-15T23:00:00Z"),
                Instant.parse("2027-05-01T10:00:00Z"));
        entityManager.persist(event);

        PriceTier regular = new PriceTier(event, "Regular", 5000, "EUR");
        PriceTier vip = new PriceTier(event, "VIP", 10000, "EUR");

        entityManager.persist(regular);
        entityManager.persist(vip);

        entityManager.persist(new Seat(event, "Main", "A", 1, regular));
        entityManager.persist(new Seat(event, "Main", "A", 2, regular));
        entityManager.persist(new Seat(event, "VIP", "B", 1, vip));

        entityManager.flush();

        UUID eventId = event.getId();

        entityManager.clear();

        // Act
        EventDetailDto result = eventService.findDetailedEvent(eventId).orElseThrow();

        // Assert
        assertThat(result.id()).isEqualTo(eventId);
        assertThat(result.title()).isEqualTo("Test Concert");
        assertThat(result.description()).isEqualTo("Live music");

        assertThat(result.venue().name()).isEqualTo("Ziggo Dome");
        assertThat(result.venue().city()).isEqualTo("Amsterdam");
        assertThat(result.venue().timezone()).isEqualTo("Europe/Amsterdam");

        assertThat(result.priceTiers()).hasSize(2);

        assertThat(result.priceTiers())
                .extracting(EventDetailDto.PriceTierDto::name)
                .containsExactlyInAnyOrder("Regular", "VIP");

        assertThat(result.priceTiers())
                .extracting(EventDetailDto.PriceTierDto::seatCount)
                .containsExactlyInAnyOrder(2L, 1L);
    }

    @Test
    void shouldReturnEmptyWhenEventDoesNotExist() {
        // Act
        var result = eventService.findDetailedEvent(UUID.randomUUID());

        // Assert
        assertThat(result).isEmpty();
    }
}
