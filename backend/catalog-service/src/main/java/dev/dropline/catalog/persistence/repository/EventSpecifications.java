package dev.dropline.catalog.persistence.repository;

import dev.dropline.catalog.domain.entity.Event;
import dev.dropline.catalog.domain.entity.EventStatus;
import java.time.Instant;
import org.springframework.data.jpa.domain.Specification;

public final class EventSpecifications {

    private EventSpecifications() {}

    public static Specification<Event> hasStatus(EventStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Event> inCity(String city) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("venue").get("city"), city);
    }

    public static Specification<Event> startsAfter(Instant date) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThan(root.<Instant>get("startsAt"), date);
    }
}
