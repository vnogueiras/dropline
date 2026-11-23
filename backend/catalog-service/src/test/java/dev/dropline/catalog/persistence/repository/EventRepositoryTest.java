package dev.dropline.catalog.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import dev.dropline.catalog.api.dto.EventSummaryDto;
import dev.dropline.catalog.domain.entity.Event;
import dev.dropline.catalog.domain.entity.Venue;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@DataJpaTest
@Testcontainers
class EventRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EntityManager entityManager;

    @TestConfiguration
    static class CacheTestConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @Test
    void shouldReturnEventSummaries() {
        // Arrange
        Venue venue = createVenue("Test Venue", "Amsterdam");
        entityManager.persist(venue);

        Event event = createEvent(venue, "Test Concert", Instant.parse("2027-06-15T19:00:00Z"));

        entityManager.persist(event);

        // Flush changes to the database
        entityManager.flush();

        // Clear the persistence context
        entityManager.clear();

        // Act
        Page<EventSummaryDto> result = eventRepository.findEventSummaries(null, PageRequest.of(0, 10));

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);

        EventSummaryDto summary = result.getContent().getFirst();

        assertThat(summary.id()).isEqualTo(event.getId());
        assertThat(summary.title()).isEqualTo("Test Concert");
        assertThat(summary.venueName()).isEqualTo("Test Venue");
        assertThat(summary.city()).isEqualTo("Amsterdam");
        assertThat(summary.startsAt()).isEqualTo(Instant.parse("2027-06-15T19:00:00Z"));
    }

    private Venue createVenue(String name, String city) {
        // Use your actual Venue constructor here
        return new Venue(name, city, "Europe/Amsterdam", createValidLayout());
    }

    private Event createEvent(Venue venue, String title, Instant startsAt) {
        // Use your actual Event constructor here
        return new Event(venue, title, null, null, startsAt, null, null);
    }

    private JsonNode createValidLayout() {
        return new ObjectMapper().createObjectNode();
    }
}
