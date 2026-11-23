package dev.dropline.catalog.service;

import dev.dropline.catalog.api.dto.EventDetailDto;
import dev.dropline.catalog.api.dto.EventSummaryDto;
import dev.dropline.catalog.api.mapper.EventMapper;
import dev.dropline.catalog.domain.entity.Event;
import dev.dropline.catalog.domain.entity.EventStatus;
import dev.dropline.catalog.persistence.repository.EventRepository;
import dev.dropline.catalog.persistence.repository.EventSpecifications;
import dev.dropline.catalog.persistence.repository.SeatRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final EventMapper eventMapper;

    public Page<EventSummaryDto> findEvents(EventStatus status, String city, Instant startsAfter, Pageable pageable) {
        Specification<Event> specification = Specification.unrestricted();

        if (status != null) {
            specification = specification.and(EventSpecifications.hasStatus(status));
        }
        if (city != null && !city.isBlank()) {
            specification = specification.and(EventSpecifications.inCity(city));
        }
        if (startsAfter != null) {
            specification = specification.and(EventSpecifications.startsAfter(startsAfter));
        }

        return eventRepository.findEventSummaries(specification, pageable);
    }

    public Optional<EventDetailDto> findDetailedEvent(UUID eventId) {
        return eventRepository.findDetailedById(eventId).map(event -> {
            Map<UUID, Long> seatCounts = seatRepository.countSeatsByTier(eventId).stream()
                    .collect(Collectors.toMap(view -> view.getTierId(), view -> view.getSeatCount()));

            return eventMapper.toDetailDto(event, seatCounts);
        });
    }
}
