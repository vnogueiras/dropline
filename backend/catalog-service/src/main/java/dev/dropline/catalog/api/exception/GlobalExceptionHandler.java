package dev.dropline.catalog.api.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Single source of error responses for catalog-service.
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} makes Spring Boot back off from registering its own
 * ProblemDetail advice, and gives us ProblemDetail responses for all standard Spring MVC exceptions
 * (type mismatch, 404 no handler, 405, 415...). Their titles and details are customised in
 * {@code messages.properties}. Validation errors are built here because they need the {@code errors} list.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Comparator<ValidationError> BY_FIELD =
            Comparator.comparing(ValidationError::field).thenComparing(ValidationError::message);

    private final ProblemMapper problemMapper;

    @ExceptionHandler(DroplineException.class)
    public ResponseEntity<ProblemDetail> handleDropline(DroplineException exception) {
        ProblemDetail problem = problemMapper.toProblem(exception);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    // ---------- validation: three sources, one response shape ----------

    /** {@code @Min}, {@code @Max}... on controller parameters (Spring MVC built-in method validation). */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        Stream<ValidationError> errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ValidationError(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())));

        return ResponseEntity.badRequest().headers(headers).body(validationProblem(errors));
    }

    /** {@code @Valid @RequestBody} (used from Phase 2 on). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Stream<ValidationError> errors = exception.getBindingResult().getAllErrors().stream()
                .map(error -> new ValidationError(fieldOf(error), error.getDefaultMessage()));

        return ResponseEntity.badRequest().headers(headers).body(validationProblem(errors));
    }

    /** Bean Validation outside Spring MVC, e.g. a {@code @Validated} service method. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception) {
        Stream<ValidationError> errors = exception.getConstraintViolations().stream()
                .map(violation -> new ValidationError(fieldOf(violation), violation.getMessage()));

        return ResponseEntity.badRequest().body(validationProblem(errors));
    }

    private static ProblemDetail validationProblem(Stream<ValidationError> errors) {
        List<ValidationError> sorted = errors.sorted(BY_FIELD).toList();

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "The request has " + sorted.size() + " invalid value(s)");
        problem.setTitle("Validation Failed");
        problem.setType(URI.create(ProblemMapper.TYPE_BASE + "validation-failed"));
        problem.setProperty("errors", sorted);
        return problem;
    }

    // FieldError -> "title"; object-level error (e.g. a class-level constraint) -> object name
    private static String fieldOf(ObjectError error) {
        return error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
    }

    // "getEvents.size" -> "size"; "create.request.title" -> "title"
    private static String fieldOf(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        return path.substring(path.lastIndexOf('.') + 1);
    }
}
