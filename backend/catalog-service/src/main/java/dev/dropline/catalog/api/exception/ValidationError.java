package dev.dropline.catalog.api.exception;

/** One entry of the {@code errors} list in a "validation-failed" ProblemDetail. */
public record ValidationError(String field, String message) {}
