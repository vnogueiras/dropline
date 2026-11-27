package dev.dropline.catalog.persistence.repository.projection;

import java.util.UUID;

public interface SeatView {

    UUID getId();

    String getSectionPath();

    String getRowLabel();

    int getSeatNumber();

    UUID getTierId();
}
