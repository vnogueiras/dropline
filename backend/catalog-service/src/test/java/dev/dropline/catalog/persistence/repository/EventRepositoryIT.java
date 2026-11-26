package dev.dropline.catalog.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import dev.dropline.catalog.api.dto.EventSummaryDto;
import dev.dropline.catalog.domain.entity.Event;
import dev.dropline.catalog.domain.entity.PriceTier;
import dev.dropline.catalog.domain.entity.Seat;
import dev.dropline.catalog.domain.entity.Venue;
import dev.dropline.catalog.persistence.repository.projection.SeatCountByTierView;
import dev.dropline.catalog.persistence.repository.projection.SeatView;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.hibernate.Hibernate;
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
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@ActiveProfiles("test")
@DataJpaTest
@Testcontainers
class EventRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SeatRepository seatRepository;

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

    @Test
    void shouldFindDetailedEventWithVenueAndPriceTiers() {
        // Arrange
        Venue venue = createVenue("Test Venue", "Amsterdam");
        entityManager.persist(venue);

        Event event = createEvent(venue, "Test Concert", Instant.parse("2027-06-15T19:00:00Z"));
        entityManager.persist(event);

        PriceTier regular = new PriceTier(event, "Regular", 5000, "EUR");
        PriceTier vip = new PriceTier(event, "VIP", 10000, "EUR");

        entityManager.persist(regular);
        entityManager.persist(vip);

        entityManager.flush();
        UUID eventId = event.getId();

        entityManager.clear();

        // Act
        Event result = eventRepository.findDetailedById(eventId).orElseThrow();

        // Assert
        assertThat(result.getId()).isEqualTo(eventId);
        assertThat(result.getVenue().getName()).isEqualTo("Test Venue");
        assertThat(result.getPriceTiers()).extracting(PriceTier::getName).containsExactlyInAnyOrder("Regular", "VIP");
    }

    @Test
    void shouldReturnEmptyWhenDetailedEventDoesNotExist() {
        // Act
        var result = eventRepository.findDetailedById(UUID.randomUUID());

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    void shouldCountSeatsByPriceTier() {
        // Arrange
        Venue venue = createVenue("Test Venue", "Amsterdam");
        entityManager.persist(venue);

        Event event = createEvent(venue, "Test Concert", Instant.parse("2027-06-15T19:00:00Z"));
        entityManager.persist(event);

        PriceTier regular = new PriceTier(event, "Regular", 5000, "EUR");
        PriceTier vip = new PriceTier(event, "VIP", 10000, "EUR");

        entityManager.persist(regular);
        entityManager.persist(vip);

        entityManager.persist(new Seat(event, "Main", "A", 1, regular));
        entityManager.persist(new Seat(event, "Main", "A", 2, regular));
        entityManager.persist(new Seat(event, "Main", "A", 3, regular));

        entityManager.persist(new Seat(event, "VIP", "B", 1, vip));
        entityManager.persist(new Seat(event, "VIP", "B", 2, vip));
        entityManager.flush();
        UUID eventId = event.getId();

        entityManager.clear();

        // Act
        var result = seatRepository.countSeatsByTier(eventId);

        // Assert
        assertThat(result).hasSize(2);

        assertThat(result)
                .extracting(SeatCountByTierView::getTierId, SeatCountByTierView::getSeatCount)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(regular.getId(), 3L),
                        org.assertj.core.groups.Tuple.tuple(vip.getId(), 2L));
    }

    @Test
    void shouldSortEventSummariesByStartsAtAscending() {
        // Arrange
        Venue venue = createVenue("Test Venue", "Amsterdam");
        entityManager.persist(venue);

        Event event1 = createEvent(venue, "Concert C", Instant.parse("2027-06-20T19:00:00Z"));

        Event event2 = createEvent(venue, "Concert A", Instant.parse("2027-06-10T19:00:00Z"));

        Event event3 = createEvent(venue, "Concert B", Instant.parse("2027-06-15T19:00:00Z"));

        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.persist(event3);

        entityManager.flush();
        entityManager.clear();

        // Act
        Page<EventSummaryDto> result = eventRepository.findEventSummaries(
                null, PageRequest.of(0, 10, Sort.by("startsAt").ascending()));

        // Assert
        assertThat(result.getContent())
                .extracting(EventSummaryDto::title)
                .containsExactly("Concert A", "Concert B", "Concert C");
    }

    @Test
    void shouldSortEventSummariesByStartsAtDescending() {
        // Arrange
        Venue venue = createVenue("Test Venue", "Amsterdam");
        entityManager.persist(venue);

        Event event1 = createEvent(venue, "Concert C", Instant.parse("2027-06-20T19:00:00Z"));
        Event event2 = createEvent(venue, "Concert A", Instant.parse("2027-06-10T19:00:00Z"));
        Event event3 = createEvent(venue, "Concert B", Instant.parse("2027-06-15T19:00:00Z"));

        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.persist(event3);

        entityManager.flush();
        entityManager.clear();

        // Act
        Page<EventSummaryDto> result = eventRepository.findEventSummaries(
                null, PageRequest.of(0, 10, Sort.by("startsAt").descending()));

        // Assert
        assertThat(result.getContent())
                .extracting(EventSummaryDto::title)
                .containsExactly("Concert C", "Concert B", "Concert A");
    }

    @Test
    void shouldSortEventSummariesByMinPriceAscending() {
        // Arrange
        Venue venue = createVenue("Test Venue", "Amsterdam");
        entityManager.persist(venue);

        Event event1 = createEvent(venue, "Concert A", Instant.parse("2027-06-20T19:00:00Z"));
        Event event2 = createEvent(venue, "Concert B", Instant.parse("2027-06-10T19:00:00Z"));
        Event event3 = createEvent(venue, "Concert C", Instant.parse("2027-06-15T19:00:00Z"));

        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.persist(event3);

        PriceTier p1 = new PriceTier(event1, "fifty", 5000, "EUR");
        PriceTier p2 = new PriceTier(event1, "eighty", 8000, "EUR");
        PriceTier p3 = new PriceTier(event2, "thirty", 3000, "EUR");
        PriceTier p4 = new PriceTier(event2, "sixty", 6000, "EUR");

        entityManager.persist(p1);
        entityManager.persist(p2);
        entityManager.persist(p3);
        entityManager.persist(p4);

        entityManager.flush();
        entityManager.clear();

        // Act
        Page<EventSummaryDto> result = eventRepository.findEventSummaries(
                null, PageRequest.of(0, 10, Sort.by("minPrice").ascending()));

        // Assert
        assertThat(result.getContent())
                .extracting(EventSummaryDto::title)
                .containsExactly("Concert B", "Concert A", "Concert C");
    }

    @Test
    void shouldSortEventSummariesByMinPriceDescending() {
        // Arrange
        Venue venue = createVenue("Test Venue", "Amsterdam");
        entityManager.persist(venue);

        Event event1 = createEvent(venue, "Concert A", Instant.parse("2027-06-20T19:00:00Z"));
        Event event2 = createEvent(venue, "Concert B", Instant.parse("2027-06-10T19:00:00Z"));
        Event event3 = createEvent(venue, "Concert C", Instant.parse("2027-06-15T19:00:00Z"));

        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.persist(event3);

        PriceTier p1 = new PriceTier(event1, "fifty", 5000, "EUR");
        PriceTier p2 = new PriceTier(event1, "eighty", 8000, "EUR");
        PriceTier p3 = new PriceTier(event2, "thirty", 3000, "EUR");
        PriceTier p4 = new PriceTier(event2, "sixty", 6000, "EUR");

        entityManager.persist(p1);
        entityManager.persist(p2);
        entityManager.persist(p3);
        entityManager.persist(p4);

        entityManager.flush();
        entityManager.clear();

        // Act
        Page<EventSummaryDto> result = eventRepository.findEventSummaries(
                null, PageRequest.of(0, 10, Sort.by("minPrice").descending()));

        // Assert
        assertThat(result.getContent())
                .extracting(EventSummaryDto::title)
                .containsExactly("Concert A", "Concert B", "Concert C");
    }

    @Test
    void shouldSortEventSummariesByStartsAtSameTimeAscending() {
        // Arrange
        Venue venue = createVenue("Test Venue", "Amsterdam");
        entityManager.persist(venue);

        Event event1 = createEvent(venue, "Concert C", Instant.parse("2027-06-20T19:00:00Z"));

        Event event2 = createEvent(venue, "Concert A", Instant.parse("2027-06-20T19:00:00Z"));

        Event event3 = createEvent(venue, "Concert B", Instant.parse("2027-06-20T19:00:00Z"));

        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.persist(event3);

        entityManager.flush();
        entityManager.clear();

        // Act
        Page<EventSummaryDto> result = eventRepository.findEventSummaries(
                null, PageRequest.of(0, 10, Sort.by("startsAt").ascending()));

        // Assert
        assertThat(result.getContent())
                .extracting(EventSummaryDto::id)
                .containsExactlyElementsOf(List.of(event1.getId(), event2.getId(), event3.getId()).stream()
                        .sorted(Comparator.comparing(UUID::toString))
                        .toList());
    }

    @Test
    void shouldPaginateEventsWithSameStartTimeWithoutDuplicates() {
        // Arrange
        Venue venue = createVenue("Test Venue", "Amsterdam");
        entityManager.persist(venue);

        Instant sameStart = Instant.parse("2027-06-20T19:00:00Z");

        Event event1 = createEvent(venue, "Concert A", sameStart);
        Event event2 = createEvent(venue, "Concert B", sameStart);
        Event event3 = createEvent(venue, "Concert C", sameStart);

        entityManager.persist(event1);
        entityManager.persist(event2);
        entityManager.persist(event3);

        entityManager.flush();
        entityManager.clear();

        Sort sort = Sort.by("startsAt").ascending();

        // Act
        Page<EventSummaryDto> firstPage = eventRepository.findEventSummaries(null, PageRequest.of(0, 2, sort));

        Page<EventSummaryDto> secondPage = eventRepository.findEventSummaries(null, PageRequest.of(1, 2, sort));

        // Assert
        List<UUID> actualIds = Stream.concat(firstPage.getContent().stream(), secondPage.getContent().stream())
                .map(EventSummaryDto::id)
                .toList();

        List<UUID> expectedIds = List.of(event1.getId(), event2.getId(), event3.getId()).stream()
                .sorted(Comparator.comparing(UUID::toString))
                .toList();

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.getTotalElements()).isEqualTo(3);
        assertThat(actualIds).containsExactlyElementsOf(expectedIds);
    }

    @Test
    void shouldFindEventWithVenueFetched() {
        Venue venue = createVenue("Test Venue", "Amsterdam");
        entityManager.persist(venue);
        Event event = createEvent(venue, "Test Concert", Instant.parse("2027-06-15T19:00:00Z"));
        entityManager.persist(event);
        entityManager.flush();
        UUID eventId = event.getId();
        entityManager.clear();

        Event result = eventRepository.findWithVenueById(eventId).orElseThrow();

        assertThat(Hibernate.isInitialized(result.getVenue())).isTrue();
        assertThat(result.getVenue().getName()).isEqualTo("Test Venue");
    }

    @Test
    void shouldReturnEmptyWhenEventWithVenueDoesNotExist() {
        assertThat(eventRepository.findWithVenueById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void shouldFindSeatsOfOneEventOrderedBySectionRowAndNumber() {
        Venue venue = createVenue("Test Venue", "Amsterdam");
        entityManager.persist(venue);
        Event event = createEvent(venue, "Test Concert", Instant.parse("2027-06-15T19:00:00Z"));
        Event otherEvent = createEvent(venue, "Other Concert", Instant.parse("2027-07-15T19:00:00Z"));
        entityManager.persist(event);
        entityManager.persist(otherEvent);

        PriceTier vip = new PriceTier(event, "VIP", 12000, "EUR");
        entityManager.persist(vip);

        // persisted out of order on purpose; one seat has no tier
        entityManager.persist(new Seat(event, "Main Hall/Floor", "B", 1, vip));
        entityManager.persist(new Seat(event, "Main Hall/Floor", "A", 2, vip));
        entityManager.persist(new Seat(event, "Main Hall/Balcony", "A", 1, null));
        entityManager.persist(new Seat(event, "Main Hall/Floor", "A", 1, vip));
        entityManager.persist(new Seat(otherEvent, "Main Hall/Floor", "A", 1, null));
        entityManager.flush();
        UUID eventId = event.getId();
        entityManager.clear();

        List<SeatView> seats = seatRepository.findSeatsByEventId(eventId);

        assertThat(seats)
                .extracting(SeatView::getSectionPath, SeatView::getRowLabel, SeatView::getSeatNumber)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Main Hall/Balcony", "A", 1),
                        org.assertj.core.groups.Tuple.tuple("Main Hall/Floor", "A", 1),
                        org.assertj.core.groups.Tuple.tuple("Main Hall/Floor", "A", 2),
                        org.assertj.core.groups.Tuple.tuple("Main Hall/Floor", "B", 1));
        assertThat(seats.getFirst().getTierId()).isNull();
        assertThat(seats.get(1).getTierId()).isEqualTo(vip.getId());
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
