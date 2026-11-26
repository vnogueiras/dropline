package dev.dropline.catalog.persistence.repository;

import dev.dropline.catalog.domain.entity.Event;
import dev.dropline.catalog.persistence.repository.projection.EventSummaryView;
import io.lettuce.core.dynamic.annotation.Param;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository
        extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event>, EventRepositoryCustom {

    @Query("""
    SELECT
        e.id AS id,
        e.title AS title,
        e.imageUrl AS imageUrl,
        e.startsAt AS startsAt,
        e.status AS status,
        v.name AS venueName,
        v.city AS city
    FROM Event e
    JOIN e.venue v
    """)
    Page<EventSummaryView> findEventSummaries(Pageable pageable);

    @EntityGraph(attributePaths = {"venue", "priceTiers"})
    Optional<Event> findDetailedById(UUID id);

    // EventRepository
    @Query("SELECT e FROM Event e JOIN FETCH e.venue WHERE e.id = :id")
    Optional<Event> findWithVenueById(@Param("id") UUID id);
}
