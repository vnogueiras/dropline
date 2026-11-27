package dev.dropline.catalog.api.exception;

public sealed class InvalidRequestException extends DroplineException permits InvalidSortException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
