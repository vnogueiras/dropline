package dev.dropline.catalog;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CatalogServiceApplicationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {}

    // Smoke test for springdoc: catches a broken springdoc / Spring Boot upgrade in CI.
    // Needs the full context: @WebMvcTest slices don't load springdoc's auto-configuration.
    @Test
    void shouldServeOpenApiSpecWithEveryEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").value(startsWith("3.")))
                .andExpect(jsonPath("$.paths['/api/events'].get").exists())
                .andExpect(jsonPath("$.paths['/api/events/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/events/{id}/layout'].get").exists())
                .andExpect(jsonPath("$.paths['/api/cities'].get").exists())
                // springdoc.paths-to-match: /api/** keeps Boot's error endpoint out of the spec
                .andExpect(jsonPath("$.paths['/error']").doesNotExist());
    }

    /**
     * Writes the spec to {@code docs/openapi/catalog.json} on every {@code verify}. The file is committed: the
     * frontend generates its TypeScript types from it, and API changes show up as a diff in PRs. CI fails if
     * the committed file is out of date.
     *
     * <p>Generated from this test (not springdoc-openapi-maven-plugin) because it already runs the full app on
     * a real database, so the build doesn't need to start and stop a separate server.
     */
    @Test
    void shouldExportOpenApiSpec() throws Exception {
        String spec = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        // set by Failsafe in catalog-service/pom.xml; fallback for running the test from the IDE
        Path output = Path.of(System.getProperty("openapi.output", "../../docs/openapi/catalog.json"));
        Files.createDirectories(output.getParent());
        Files.writeString(output, spec.strip() + "\n", StandardCharsets.UTF_8);
    }

    @Test
    void shouldRedirectToSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }
}
