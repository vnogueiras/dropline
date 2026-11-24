package dev.dropline.catalog.persistence.repository;

import dev.dropline.catalog.api.dto.EventSummaryDto;
import dev.dropline.catalog.api.exception.InvalidSortException;
import dev.dropline.catalog.domain.entity.Event;
import dev.dropline.catalog.domain.entity.PriceTier;
import dev.dropline.catalog.domain.entity.Venue;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@RequiredArgsConstructor
public class EventRepositoryCustomImpl implements EventRepositoryCustom {

    private final EntityManager entityManager;

    private static final Set<String> ALLOWED_SORT_PROPERTIES =
            Set.of("title", "startsAt", "status", "venueName", "city", "minPrice");

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

        List<String> allowedSortProperties = List.of("title", "startsAt", "status", "venueName", "city", "minPrice");

        for (Sort.Order sortOrder : pageable.getSort()) {
            String property = sortOrder.getProperty();

            if (property.equals("minPrice")) {
                orders.addAll(buildMinPriceOrders(sortOrder.isAscending(), eventRoot, query, criteriaBuilder));
            } else {
                orders.add(buildOrder(property, sortOrder.isAscending(), eventRoot, venueJoin, query, criteriaBuilder));
            }
        }

        if (pageable.getSort().stream().noneMatch(order -> order.getProperty().equals("id"))) {
            orders.add(criteriaBuilder.asc(eventRoot.get("id")));
        }

        query.orderBy(orders);

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

    private Order buildOrder(
            String property,
            boolean ascending,
            Root<Event> eventRoot,
            Join<Event, Venue> venueJoin,
            CriteriaQuery<?> query,
            CriteriaBuilder criteriaBuilder) {

        if (!ALLOWED_SORT_PROPERTIES.contains(property)) {
            throw new InvalidSortException(property);
        }

        return switch (property) {
            case "title", "startsAt", "status", "id" ->
                ascending
                        ? criteriaBuilder.asc(eventRoot.get(property))
                        : criteriaBuilder.desc(eventRoot.get(property));

            case "city" ->
                ascending ? criteriaBuilder.asc(venueJoin.get("city")) : criteriaBuilder.desc(venueJoin.get("city"));

            case "venueName" ->
                ascending ? criteriaBuilder.asc(venueJoin.get("name")) : criteriaBuilder.desc(venueJoin.get("name"));

            case "minPrice" -> buildMinPriceOrder(ascending, eventRoot, query, criteriaBuilder);

            default -> throw new InvalidSortException(property);
        };
    }

    private Order buildMinPriceOrder(
            boolean ascending, Root<Event> eventRoot, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {

        Subquery<Integer> minPriceQuery = query.subquery(Integer.class);
        Root<PriceTier> priceTierRoot = minPriceQuery.from(PriceTier.class);

        minPriceQuery.select(criteriaBuilder.min(priceTierRoot.get("priceCents")));
        minPriceQuery.where(criteriaBuilder.equal(priceTierRoot.get("event").get("id"), eventRoot.get("id")));

        Expression<Integer> nullRank = criteriaBuilder
                .<Integer>selectCase()
                .when(criteriaBuilder.isNull(minPriceQuery), 1)
                .otherwise(0);

        // Keep events without prices last, regardless of sort direction.
        return criteriaBuilder.asc(nullRank);
    }

    private List<Order> buildMinPriceOrders(
            boolean ascending, Root<Event> eventRoot, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {

        Subquery<Integer> minPriceQuery = query.subquery(Integer.class);
        Root<PriceTier> priceTierRoot = minPriceQuery.from(PriceTier.class);

        minPriceQuery.select(criteriaBuilder.min(priceTierRoot.get("priceCents")));
        minPriceQuery.where(criteriaBuilder.equal(priceTierRoot.get("event").get("id"), eventRoot.get("id")));

        Expression<Integer> nullRank = criteriaBuilder
                .<Integer>selectCase()
                .when(criteriaBuilder.isNull(minPriceQuery), 1)
                .otherwise(0);

        return List.of(
                criteriaBuilder.asc(nullRank),
                ascending ? criteriaBuilder.asc(minPriceQuery) : criteriaBuilder.desc(minPriceQuery));
    }
}
