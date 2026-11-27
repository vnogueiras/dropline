package dev.dropline.catalog.api.exception;

public final class InvalidSortException extends InvalidRequestException {

    public InvalidSortException(String sort) {
        super("Sorting by '" + sort + "' is not supported");
    }
}
