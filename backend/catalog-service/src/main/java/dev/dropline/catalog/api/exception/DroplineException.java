package dev.dropline.catalog.api.exception;

public abstract sealed class DroplineException extends RuntimeException
        permits ConflictException, InvalidRequestException, NotFoundException {
    protected DroplineException(String message) {
        super(message);
    }
}
