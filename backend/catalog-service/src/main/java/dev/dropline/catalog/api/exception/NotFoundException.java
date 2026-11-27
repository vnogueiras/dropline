package dev.dropline.catalog.api.exception;

import java.util.UUID;
import lombok.Getter;

@Getter
public final class NotFoundException extends DroplineException {

    private final UUID resourceId;
    private final String resource;

    public NotFoundException(String resource, UUID resourceId) {
        super(resource + " " + resourceId + " was not found");
        this.resourceId = resourceId;
        this.resource = resource;
    }
}
