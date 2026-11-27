package dev.dropline.catalog.api.exception;

import java.net.URI;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/**
 * Turns every {@link DroplineException} into an RFC 9457 {@link ProblemDetail}.
 *
 * <p>The switch has no {@code default}: {@code DroplineException} is sealed, so the compiler
 * knows every possible subtype. Adding a new subclass breaks compilation until it is mapped here.
 */
@Component
public class ProblemMapper {

    static final String TYPE_BASE = "https://dropline.dev/problems/";

    public ProblemDetail toProblem(DroplineException exception) {
        return switch (exception) {
            case NotFoundException notFound -> {
                ProblemDetail problem = problem(
                        HttpStatus.NOT_FOUND,
                        notFound.getResource() + " Not Found",
                        slug(notFound.getResource()) + "-not-found",
                        notFound.getMessage());
                problem.setProperty("resource", notFound.getResource());
                problem.setProperty("resourceId", notFound.getResourceId());
                yield problem;
            }
            // must come before InvalidRequestException, which it extends ("dominated case label")
            case InvalidSortException invalidSort ->
                problem(HttpStatus.BAD_REQUEST, "Invalid Sort Parameter", "invalid-sort", invalidSort.getMessage());
            case InvalidRequestException invalidRequest ->
                problem(HttpStatus.BAD_REQUEST, "Invalid Request", "invalid-request", invalidRequest.getMessage());
            case ConflictException conflict ->
                problem(HttpStatus.CONFLICT, "Conflict", "conflict", conflict.getMessage());
        };
    }

    private static ProblemDetail problem(HttpStatus status, String title, String typeSlug, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(TYPE_BASE + typeSlug));
        return problem;
    }

    // "Event" -> "event", "Price Tier" -> "price-tier"
    private static String slug(String resource) {
        return resource.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", "-");
    }
}
