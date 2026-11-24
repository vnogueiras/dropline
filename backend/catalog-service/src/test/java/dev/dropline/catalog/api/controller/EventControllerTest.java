package dev.dropline.catalog.api.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.dropline.catalog.api.exception.InvalidSortException;
import dev.dropline.catalog.domain.entity.EventStatus;
import dev.dropline.catalog.service.EventService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EventController.class)
@Import(EventControllerTest.TestCacheConfiguration.class)
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
}
