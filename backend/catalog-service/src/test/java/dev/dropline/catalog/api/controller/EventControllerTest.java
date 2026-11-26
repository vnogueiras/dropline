package dev.dropline.catalog.api.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.dropline.catalog.api.dto.EventDetailDto;
import dev.dropline.catalog.api.dto.EventLayoutDto;
import dev.dropline.catalog.api.exception.InvalidSortException;
import dev.dropline.catalog.config.HttpCachingConfig;
import dev.dropline.catalog.domain.entity.EventStatus;
import dev.dropline.catalog.domain.layout.SeatBlock;
import dev.dropline.catalog.domain.layout.Section;
import dev.dropline.catalog.service.EventService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EventController.class)
@Import({EventControllerTest.TestCacheConfiguration.class, HttpCachingConfig.class})
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @TestConfiguration(proxyBeanMethods = false)
    static class TestCacheConfiguration {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    private void mockEmptyPage() {
        when(eventService.findEvents(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 12), 0));
    }

    @Test
    void shouldReturnEventsWithDefaultParameters() throws Exception {
        mockEmptyPage();

        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page.size").value(12))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(0));

        verify(eventService)
                .findEvents(
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        argThat(pageable -> pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 12
                                && pageable.getSort().getOrderFor("startsAt").isAscending()));
    }

    @Test
    void shouldFilterEventsByCityAndStatus() throws Exception {
        mockEmptyPage();

        mockMvc.perform(get("/api/events").param("city", "Barcelona").param("status", "ON_SALE"))
                .andExpect(status().isOk());

        verify(eventService)
                .findEvents(eq(Set.of(EventStatus.ON_SALE)), eq("Barcelona"), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void shouldConvertDateParametersToInstants() throws Exception {
        mockEmptyPage();

        mockMvc.perform(get("/api/events").param("from", "2026-12-01").param("to", "2026-12-31"))
                .andExpect(status().isOk());

        verify(eventService)
                .findEvents(
                        isNull(),
                        isNull(),
                        eq(Instant.parse("2026-12-01T00:00:00Z")),
                        eq(Instant.parse("2027-01-01T00:00:00Z")),
                        any(Pageable.class));
    }

    @Test
    void shouldAcceptBlankCity() throws Exception {
        mockEmptyPage();

        mockMvc.perform(get("/api/events").param("city", " ")).andExpect(status().isOk());

        verify(eventService).findEvents(isNull(), eq(" "), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void shouldApplyCustomPaginationAndSorting() throws Exception {
        mockEmptyPage();

        mockMvc.perform(get("/api/events").param("page", "1").param("size", "5").param("sort", "minPrice,desc"))
                .andExpect(status().isOk());

        verify(eventService)
                .findEvents(
                        any(),
                        any(),
                        any(),
                        any(),
                        argThat(pageable -> pageable.getPageNumber() == 1
                                && pageable.getPageSize() == 5
                                && pageable.getSort().getOrderFor("minPrice").isDescending()));
    }

    @Test
    void shouldRejectNegativePage() throws Exception {
        mockMvc.perform(get("/api/events").param("page", "-1")).andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectPageSizeAboveMaximum() throws Exception {
        mockMvc.perform(get("/api/events").param("size", "500")).andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectZeroPageSize() throws Exception {
        mockMvc.perform(get("/api/events").param("size", "0")).andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/events").param("status", "INVALID")).andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidDateRange() throws Exception {
        mockMvc.perform(get("/api/events").param("from", "2026-12-20").param("to", "2026-12-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidSortDirection() throws Exception {
        mockMvc.perform(get("/api/events").param("sort", "startsAt,sideways")).andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400WhenSortPropertyIsInvalid() throws Exception {
        when(eventService.findEvents(
                        isNull(),
                        nullable(String.class),
                        nullable(Instant.class),
                        nullable(Instant.class),
                        any(Pageable.class)))
                .thenThrow(new InvalidSortException("foo"));

        mockMvc.perform(get("/api/events").param("sort", "foo"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Sort Parameter"))
                .andExpect(jsonPath("$.detail").value("Sorting by 'foo' is not supported"));
    }

    // ---------- GET /api/events/{id} ----------

    private static final UUID EVENT_ID = UUID.fromString("3f2c9e1a-5b7d-4c11-9a0e-2d8f6b4c1e77");

    private EventDetailDto sampleDetail() {
        return new EventDetailDto(
                EVENT_ID,
                "Electronic Nights",
                "Live music",
                "/images/events/electronic-nights.jpg",
                Instant.parse("2027-06-15T19:00:00Z"),
                Instant.parse("2027-06-15T22:00:00Z"),
                Instant.parse("2027-05-15T10:00:00Z"),
                EventStatus.ON_SALE,
                new EventDetailDto.VenueDto(UUID.randomUUID(), "Ziggo Dome", "Amsterdam", "Europe/Amsterdam"),
                List.of(new EventDetailDto.PriceTierDto(UUID.randomUUID(), "VIP", 12000, "EUR", 20)));
    }

    @Test
    void shouldReturnEventDetailWithCacheHeaders() throws Exception {
        when(eventService.findDetailedEvent(EVENT_ID)).thenReturn(Optional.of(sampleDetail()));

        mockMvc.perform(get("/api/events/{id}", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=30, public"))
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(jsonPath("$.id").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.title").value("Electronic Nights"))
                .andExpect(jsonPath("$.venue.city").value("Amsterdam"))
                .andExpect(jsonPath("$.priceTiers[0].seatCount").value(20));
    }

    @Test
    void shouldReturn304WhenETagMatches() throws Exception {
        when(eventService.findDetailedEvent(EVENT_ID)).thenReturn(Optional.of(sampleDetail()));

        String etag = mockMvc.perform(get("/api/events/{id}", EVENT_ID))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.ETAG);

        mockMvc.perform(get("/api/events/{id}", EVENT_ID).header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified())
                .andExpect(content().string(""));
    }

    @Test
    void shouldReturn404ProblemDetailWhenEventDoesNotExist() throws Exception {
        when(eventService.findDetailedEvent(EVENT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/events/{id}", EVENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Event Not Found"))
                .andExpect(jsonPath("$.type").value("https://dropline.dev/problems/event-not-found"))
                .andExpect(jsonPath("$.eventId").value(EVENT_ID.toString()));
    }

    @Test
    void shouldReturn400WhenEventIdIsNotAUuid() throws Exception {
        mockMvc.perform(get("/api/events/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Request Parameter"));

        verifyNoInteractions(eventService);
    }

    // ---------- GET /api/events/{id}/layout ----------

    @Test
    void shouldReturnLayoutWithLongerCacheControl() throws Exception {
        UUID tierId = UUID.randomUUID();
        EventLayoutDto layout = new EventLayoutDto(
                EVENT_ID,
                "Ziggo Dome",
                new Section("Main Hall", List.of(new SeatBlock("Floor", List.of(new SeatBlock.Row("A", 2))))),
                List.of(
                        new EventLayoutDto.SeatDto(UUID.randomUUID(), "Main Hall/Floor", "A", 1, tierId),
                        new EventLayoutDto.SeatDto(UUID.randomUUID(), "Main Hall/Floor", "A", 2, tierId)));
        when(eventService.findLayout(EVENT_ID)).thenReturn(Optional.of(layout));

        mockMvc.perform(get("/api/events/{id}/layout", EVENT_ID))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=300, public"))
                .andExpect(jsonPath("$.venueName").value("Ziggo Dome"))
                .andExpect(jsonPath("$.layout.name").value("Main Hall"))
                .andExpect(jsonPath("$.layout.children[0].rows[0].seatCount").value(2))
                .andExpect(jsonPath("$.seats.length()").value(2))
                .andExpect(jsonPath("$.seats[1].seatNumber").value(2));
    }

    @Test
    void shouldReturn404WhenLayoutEventDoesNotExist() throws Exception {
        when(eventService.findLayout(EVENT_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/events/{id}/layout", EVENT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.eventId").value(EVENT_ID.toString()));
    }
}
