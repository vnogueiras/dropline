package dev.dropline.catalog.persistence.repository.projection;

import java.util.UUID;

public interface SeatCountByTierView {

    UUID getTierId();

    Long getSeatCount();
}
