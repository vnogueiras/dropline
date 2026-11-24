package dev.dropline.catalog.api.exception;

public class InvalidSortException extends RuntimeException {

    public InvalidSortException(String sort) {
        super("Sorting by '" + sort + "' is not supported");
    }
}
