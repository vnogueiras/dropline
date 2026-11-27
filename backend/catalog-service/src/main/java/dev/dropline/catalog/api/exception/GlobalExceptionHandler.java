package dev.dropline.catalog.api.exception;

import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Single source of error responses for catalog-service.
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} makes Spring Boot back off from registering its own
 * ProblemDetail advice, and gives us ProblemDetail responses for all standard Spring MVC exceptions
 * (type mismatch, method validation, 404 no handler, 405, 415...). Their titles and details are
 * customised in {@code messages.properties}, not in code.
 */
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final ProblemMapper problemMapper;

    @ExceptionHandler(DroplineException.class)
    public ResponseEntity<ProblemDetail> handleDropline(DroplineException exception) {
        ProblemDetail problem = problemMapper.toProblem(exception);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    // Not covered by ResponseEntityExceptionHandler: thrown by Bean Validation outside Spring MVC's
    // built-in method validation (e.g. a @Validated service method).
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Validation Failed");
        problem.setType(URI.create(ProblemMapper.TYPE_BASE + "validation-failed"));
        return ResponseEntity.badRequest().body(problem);
    }
}
