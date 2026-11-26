package dev.dropline.catalog.persistence.repository.projection;

import java.util.UUID;

public interface SeatView {
    // s.id AS id, s.sectionPath AS sectionPath, s.rowLabel AS rowLabel,
    //               s.seatNumber AS seatNumber, s.tier.id AS tierId

    UUID getId();

    String getSectionPath();

    String getRowLabel();

    int getSeatNumber();

    UUID getTierId();
}
