package dev.dropline.catalog.api.controller;

import dev.dropline.catalog.api.dto.EventDetailDto;
import dev.dropline.catalog.api.dto.EventLayoutDto;
import dev.dropline.catalog.api.dto.EventSummaryDto;
import dev.dropline.catalog.api.exception.NotFoundException;
import dev.dropline.catalog.domain.entity.EventStatus;
import dev.dropline.catalog.service.EventService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController()
@RequestMapping("/api/events")
@Validated
@RequiredArgsConstructor
public class EventController {

    final EventService eventService;

    @GetMapping()
    public PagedModel<EventSummaryDto> getEvents(
            @RequestParam(required = false) String city,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) @Max(50) int size,
            @RequestParam(defaultValue = "startsAt,asc") String sort) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("The 'to' date must not be before the 'from' date");
        }

        Set<EventStatus> statuses = status == null ? null : Set.of(status);
        Instant fromInstant = from != null ? from.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant toInstant =
                to != null ? to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;

        Pageable pageable = PageRequest.of(page, size, parseSort(sort));

        Page<EventSummaryDto> result = eventService.findEvents(statuses, city, fromInstant, toInstant, pageable);

        return new PagedModel<>(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDetailDto> getEvent(@PathVariable UUID id) {
        EventDetailDto event = eventService.findDetailedEvent(id).orElseThrow(() -> new NotFoundException(id));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(30)).cachePublic())
                .body(event);
    }

    @GetMapping("/{id}/layout")
    public ResponseEntity<EventLayoutDto> getEventLayout(@PathVariable UUID id) throws NotFoundException {
        EventLayoutDto eventLayout = eventService.findLayout(id).orElseThrow(() -> new NotFoundException(id));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofSeconds(300)).cachePublic())
                .body(eventLayout);
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",", -1);

        String property = parts[0].strip();

        Sort.Direction direction = parts.length == 2 ? Sort.Direction.fromString(parts[1].strip()) : Sort.Direction.ASC;

        return Sort.by(direction, property);
    }
}
