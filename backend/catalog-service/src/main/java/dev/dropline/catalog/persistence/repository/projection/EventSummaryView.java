package dev.dropline.catalog.persistence.repository.projection;

import dev.dropline.catalog.domain.entity.EventStatus;
import java.time.Instant;
import java.util.UUID;

public interface EventSummaryView {
    UUID getId();

    String getTitle();

    String getImageUrl();

    Instant getStartsAt();

    EventStatus getStatus();

    String getVenueName();

    String getCity();
}
