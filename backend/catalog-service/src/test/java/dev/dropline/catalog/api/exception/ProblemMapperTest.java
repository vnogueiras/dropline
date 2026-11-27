package dev.dropline.catalog.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.ProblemDetail;

class ProblemMapperTest {

    private static final UUID EVENT_ID = UUID.fromString("3f2c9e1a-5b7d-4c11-9a0e-2d8f6b4c1e77");

    private final ProblemMapper mapper = new ProblemMapper();

    /** One row per concrete {@link DroplineException} type: exception, status, title, type slug. */
    static Stream<Arguments> everyException() {
        return Stream.of(
                Arguments.of(new NotFoundException("Event", EVENT_ID), 404, "Event Not Found", "event-not-found"),
                Arguments.of(new InvalidRequestException("bad input"), 400, "Invalid Request", "invalid-request"),
                Arguments.of(new InvalidSortException("price"), 400, "Invalid Sort Parameter", "invalid-sort"),
                Arguments.of(new ConflictException("seat already held"), 409, "Conflict", "conflict"));
    }

    @ParameterizedTest(name = "{0} -> {1} {2}")
    @MethodSource("everyException")
    void shouldMapEveryExceptionToItsProblem(DroplineException exception, int status, String title, String typeSlug) {
        ProblemDetail problem = mapper.toProblem(exception);

        assertThat(problem.getStatus()).isEqualTo(status);
        assertThat(problem.getTitle()).isEqualTo(title);
        assertThat(problem.getType()).isEqualTo(URI.create("https://dropline.dev/problems/" + typeSlug));
        assertThat(problem.getDetail()).isEqualTo(exception.getMessage());
    }

    @Test
    void shouldAddResourceAndIdToNotFoundProblem() {
        ProblemDetail problem = mapper.toProblem(new NotFoundException("Event", EVENT_ID));

        assertThat(problem.getDetail()).isEqualTo("Event " + EVENT_ID + " was not found");
        assertThat(problem.getProperties()).containsEntry("resource", "Event").containsEntry("resourceId", EVENT_ID);
    }

    @Test
    void shouldBuildTypeSlugFromMultiWordResource() {
        ProblemDetail problem = mapper.toProblem(new NotFoundException("Price Tier", EVENT_ID));

        assertThat(problem.getTitle()).isEqualTo("Price Tier Not Found");
        assertThat(problem.getType()).isEqualTo(URI.create("https://dropline.dev/problems/price-tier-not-found"));
    }

    @Test
    void shouldMapSubclassBeforeParent() {
        // InvalidSortException extends InvalidRequestException: it must get its own problem, not the parent's
        ProblemDetail problem = mapper.toProblem(new InvalidSortException("price"));

        assertThat(problem.getTitle()).isEqualTo("Invalid Sort Parameter");
        assertThat(problem.getDetail()).isEqualTo("Sorting by 'price' is not supported");
    }

    /**
     * The compiler already forces {@code toProblem} to handle every subtype. This test forces
     * {@link #everyException()} to keep up too: adding a new subclass fails here until it gets a row.
     */
    @Test
    void shouldCoverEveryConcreteSubclassOfTheSealedHierarchy() {
        Set<Class<?>> tested =
                everyException().map(arguments -> arguments.get()[0].getClass()).collect(Collectors.toSet());

        assertThat(tested).isEqualTo(concreteSubtypes(DroplineException.class));
    }

    // Walks the sealed hierarchy recursively (InvalidRequestException is itself sealed and concrete).
    private static Set<Class<?>> concreteSubtypes(Class<?> type) {
        Stream<Class<?>> self = Modifier.isAbstract(type.getModifiers()) ? Stream.empty() : Stream.of(type);
        Stream<Class<?>> children = type.isSealed()
                ? Arrays.stream(type.getPermittedSubclasses()).flatMap(child -> concreteSubtypes(child).stream())
                : Stream.empty();
        return Stream.concat(self, children).collect(Collectors.toSet());
    }
}
