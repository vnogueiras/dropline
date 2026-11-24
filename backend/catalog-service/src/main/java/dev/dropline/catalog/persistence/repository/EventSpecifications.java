package dev.dropline.catalog.persistence.repository;

import dev.dropline.catalog.domain.entity.Event;
import dev.dropline.catalog.domain.entity.EventStatus;
import java.time.Instant;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

public final class EventSpecifications {

    private EventSpecifications() {}

    public static Specification<Event> hasStatus(EventStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Event> cityEquals(String city) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("venue").get("city"), city);
    }

    public static Specification<Event> startsOnOrAfter(Instant date) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.<Instant>get("startsAt"), date);
    }

    public static Specification<Event> startsBetween(Instant dateFrom, Instant dateTo) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.between(root.<Instant>get("startsAt"), dateFrom, dateTo);
    }

    public static Specification<Event> statusIn(Set<EventStatus> statuses) {
        return (root, query, criteriaBuilder) -> root.<EventStatus>get("status").in(statuses);
    }
}
