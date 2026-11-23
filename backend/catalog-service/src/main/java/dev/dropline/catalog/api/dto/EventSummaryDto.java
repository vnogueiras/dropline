package dev.dropline.catalog.api.dto;

import dev.dropline.catalog.domain.entity.EventStatus;
import java.time.Instant;
import java.util.UUID;

public record EventSummaryDto(
        UUID id, String title, String imageUrl, Instant startsAt, EventStatus status, String venueName, String city) {}
