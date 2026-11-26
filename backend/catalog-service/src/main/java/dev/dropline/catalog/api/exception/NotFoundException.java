package dev.dropline.catalog.api.exception;

import java.util.UUID;
import lombok.Getter;

@Getter
public class NotFoundException extends RuntimeException {

    private final UUID eventId;

    public NotFoundException(UUID eventId) {
        super("OBject " + eventId + " was not found");
        this.eventId = eventId;
    }
}
