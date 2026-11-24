package dev.dropline.catalog.service;

import dev.dropline.catalog.api.dto.EventDetailDto;
import dev.dropline.catalog.api.dto.EventSummaryDto;
import dev.dropline.catalog.api.exception.InvalidSortException;
import dev.dropline.catalog.api.mapper.EventMapper;
import dev.dropline.catalog.domain.entity.Event;
import dev.dropline.catalog.domain.entity.EventStatus;
import dev.dropline.catalog.persistence.repository.EventRepository;
import dev.dropline.catalog.persistence.repository.EventSpecifications;
import dev.dropline.catalog.persistence.repository.SeatRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final EventMapper eventMapper;

    private static final Set<String> ALLOWED_SORTS = Set.of("startsAt", "title", "minPrice");

    public Page<EventSummaryDto> findEvents(
            Set<EventStatus> statuses, String city, Instant from, Instant to, Pageable pageable) {
        Specification<Event> specification = Specification.unrestricted();

        var statusesLocal = Optional.ofNullable(statuses)
                .filter(s -> !s.isEmpty())
                .orElse(Set.of(EventStatus.ON_SALE, EventStatus.PUBLISHED));

        specification = specification.and(EventSpecifications.statusIn(statusesLocal));

        var cityLocal = city;
        if (cityLocal != null && !cityLocal.isBlank()) {
            cityLocal = cityLocal.strip();
            specification = specification.and(EventSpecifications.cityEquals(cityLocal));
        }

        var fromLocal = from;
        if (fromLocal == null) {
            fromLocal = Instant.now();
        }

        specification = specification.and(EventSpecifications.startsOnOrAfter(fromLocal));

        if (to != null) {
            specification = specification.and(EventSpecifications.startsBefore(to));
        }

        return eventRepository.findEventSummaries(specification, mapSort(pageable));
    }

    public Optional<EventDetailDto> findDetailedEvent(UUID eventId) {
        return eventRepository.findDetailedById(eventId).map(event -> {
            Map<UUID, Long> seatCounts = seatRepository.countSeatsByTier(eventId).stream()
                    .collect(Collectors.toMap(view -> view.getTierId(), view -> view.getSeatCount()));

            return eventMapper.toDetailDto(event, seatCounts);
        });
    }

    private String mapSortProperty(String sort) {
        if (!ALLOWED_SORTS.contains(sort)) {
            throw new InvalidSortException(sort);
        }
        return switch (sort) {
            case "startsAt", "title" -> sort;
            case "minPrice" -> "minPriceCents";
            default -> throw new InvalidSortException(sort);
        };
    }

    private Pageable mapSort(Pageable pageable) {
        Sort sort = Sort.by(pageable.getSort().stream()
                .map(order -> new Sort.Order(order.getDirection(), mapSortProperty(order.getProperty())))
                .toList());

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
