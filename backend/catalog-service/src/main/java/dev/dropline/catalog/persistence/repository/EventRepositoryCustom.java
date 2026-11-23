package dev.dropline.catalog.persistence.repository;

import dev.dropline.catalog.api.dto.EventSummaryDto;
import dev.dropline.catalog.domain.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface EventRepositoryCustom {

    Page<EventSummaryDto> findEventSummaries(Specification<Event> specification, Pageable pageable);
}
