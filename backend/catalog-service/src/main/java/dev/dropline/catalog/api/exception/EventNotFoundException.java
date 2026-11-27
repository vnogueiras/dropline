package dev.dropline.catalog.api.exception;

import java.util.UUID;
import lombok.Getter;

@Getter
public class EventNotFoundException extends RuntimeException {

    private final UUID eventId;

    public EventNotFoundException(UUID eventId) {
        super("Event " + eventId + " was not found");
        this.eventId = eventId;
    }
}
