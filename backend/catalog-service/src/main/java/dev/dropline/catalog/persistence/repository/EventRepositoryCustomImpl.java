package dev.dropline.catalog.persistence.repository;

import dev.dropline.catalog.api.dto.EventSummaryDto;
import dev.dropline.catalog.domain.entity.Event;
import dev.dropline.catalog.domain.entity.Venue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@RequiredArgsConstructor
public class EventRepositoryCustomImpl implements EventRepositoryCustom {

    private final EntityManager entityManager;

    public Page<EventSummaryDto> findEventSummaries(Specification<Event> specification, Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        List<EventSummaryDto> content = fetchContent(specification, pageable, criteriaBuilder);

        long total = countEvents(specification, criteriaBuilder);

        return new PageImpl<>(content, pageable, total);
    }

    private List<EventSummaryDto> fetchContent(
            Specification<Event> specification, Pageable pageable, CriteriaBuilder criteriaBuilder) {
        CriteriaQuery<EventSummaryDto> query = criteriaBuilder.createQuery(EventSummaryDto.class);

        Root<Event> eventRoot = query.from(Event.class);
        Join<Event, Venue> venueJoin = eventRoot.join("venue");

        query.select(criteriaBuilder.construct(
                EventSummaryDto.class,
                eventRoot.get("id"),
                eventRoot.get("title"),
                eventRoot.get("imageUrl"),
                eventRoot.get("startsAt"),
                eventRoot.get("status"),
                venueJoin.get("name"),
                venueJoin.get("city")));

        applySpecification(specification, eventRoot, query, criteriaBuilder);

        List<Order> orders = new ArrayList<>();

        List<String> allowedSortProperties =
                List.of("id", "title", "imageUrl", "startsAt", "status", "venueName", "city");

        for (Sort.Order sortOrder : pageable.getSort()) {
            String property = sortOrder.getProperty();

            if (!allowedSortProperties.contains(property)) {
                throw new IllegalArgumentException("Invalid sort property: " + property);
            }

            if (property.equals("city")) {
                if (sortOrder.isAscending()) {
                    orders.add(criteriaBuilder.asc(venueJoin.get("city")));
                } else {
                    orders.add(criteriaBuilder.desc(venueJoin.get("city")));
                }
            } else if (property.equals("venueName")) {
                if (sortOrder.isAscending()) {
                    orders.add(criteriaBuilder.asc(venueJoin.get("name")));
                } else {
                    orders.add(criteriaBuilder.desc(venueJoin.get("name")));
                }
            } else {
                if (sortOrder.isAscending()) {
                    orders.add(criteriaBuilder.asc(eventRoot.get(property)));
                } else {
                    orders.add(criteriaBuilder.desc(eventRoot.get(property)));
                }
            }
        }

        if (!orders.isEmpty()) {
            query.orderBy(orders);
        }

        TypedQuery<EventSummaryDto> typedQuery = entityManager.createQuery(query);

        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        return typedQuery.getResultList();
    }

    private long countEvents(Specification<Event> specification, CriteriaBuilder criteriaBuilder) {
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);

        Root<Event> countRoot = countQuery.from(Event.class);

        applySpecification(specification, countRoot, countQuery, criteriaBuilder);

        countQuery.select(criteriaBuilder.count(countRoot));

        return entityManager.createQuery(countQuery).getSingleResult();
    }

    private <T> void applySpecification(
            Specification<Event> specification,
            Root<Event> root,
            CriteriaQuery<T> query,
            CriteriaBuilder criteriaBuilder) {
        if (specification == null) {
            return;
        }

        Predicate predicate = specification.toPredicate(root, query, criteriaBuilder);

        if (predicate != null) {
            query.where(predicate);
        }
    }
}
