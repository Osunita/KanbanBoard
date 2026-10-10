package dev.osuna.kanban.web;

import com.jayway.jsonpath.JsonPath;
import dev.osuna.kanban.support.PostgresContainerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the project API against real PostgreSQL (SPEC 12.1/12.2):
 * TEST-001…004 plus the Fase 1 error cases (E-05, E-14, E-15, 404/405/415, trimmed length).
 * Docker is required; each test starts from a clean database (D5).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
class ProjectApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        // Cascade removes every task of the deleted projects.
        jdbcTemplate.update("DELETE FROM project");
    }

    // --- TEST-001: valid creation -------------------------------------------

    @Test
    void createProjectReturns201WithLocationAndBody() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Demo\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/projects/\\d+")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Demo"))
                .andExpect(jsonPath("$.description").isEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    // --- TEST-002: blank name ------------------------------------------------

    @Test
    void blankNameReturns400WithFieldName() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
    }

    // --- TEST-003: missing project ------------------------------------------

    @Test
    void getMissingProjectReturns404WithDetail() throws Exception {
        mockMvc.perform(get("/api/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Project 999 not found"));
    }

    // --- TEST-004: cascade delete (adapted per D2) --------------------------

    @Test
    void deleteProjectRemovesItsTasksViaCascade() throws Exception {
        long projectId = createProject("Con tareas");

        // Seed 2 task rows directly (task API belongs to Fase 2).
        jdbcTemplate.update(
                "INSERT INTO task (project_id, title, status, priority, position) VALUES "
                        + "(?, 'A', 'TODO', 'MEDIUM', 0), (?, 'B', 'TODO', 'MEDIUM', 1)",
                projectId, projectId);
        Integer seeded = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM task WHERE project_id = ?", Integer.class, projectId);
        assertThat(seeded).isEqualTo(2);

        mockMvc.perform(delete("/api/projects/" + projectId))
                .andExpect(status().isNoContent());

        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM task WHERE project_id = ?", Integer.class, projectId);
        assertThat(remaining).isZero();
        // Deferred to Fase 2 (D2): GET /api/projects/{id}/tasks must also 404.
    }

    // --- List (AC-003, ordering) --------------------------------------------

    @Test
    void emptyListReturns200WithBody() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void listReturnsMostRecentFirst() throws Exception {
        // Explicit distinct createdAt values (no sleeps → no clock-granularity flake).
        seedProjectWithCreatedAt("Primero", Timestamp.from(Instant.parse("2026-01-01T10:00:00Z")));
        seedProjectWithCreatedAt("Segundo", Timestamp.from(Instant.parse("2026-01-01T11:00:00Z")));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Segundo"))
                .andExpect(jsonPath("$[1].name").value("Primero"));
    }

    // --- Read / update / delete by id ---------------------------------------

    @Test
    void getExistingProjectReturns200() throws Exception {
        long id = createProject("Existente");

        mockMvc.perform(get("/api/projects/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Existente"));
    }

    @Test
    void putReplacesNameAndDescription() throws Exception {
        long id = createProject("Old");
        jdbcTemplate.update("UPDATE project SET description = 'old description' WHERE id = ?", id);

        mockMvc.perform(put("/api/projects/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"New\",\"description\":\"d\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New"))
                .andExpect(jsonPath("$.description").value("d"));
    }

    @Test
    void putWithoutDescriptionClearsIt() throws Exception {
        long id = createProject("Con descripcion");
        jdbcTemplate.update("UPDATE project SET description = 'old description' WHERE id = ?", id);

        mockMvc.perform(put("/api/projects/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("X"))
                .andExpect(jsonPath("$.description").isEmpty());
    }

    @Test
    void putMissingProjectReturns404() throws Exception {
        mockMvc.perform(put("/api/projects/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Project 999 not found"));
    }

    @Test
    void deleteMissingProjectReturns404() throws Exception {
        mockMvc.perform(delete("/api/projects/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Project 999 not found"));
    }

    // --- Validation boundaries (E-05) ---------------------------------------

    @Test
    void nameOfExactly100CharsIsAccepted() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "a".repeat(100) + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void nameOf101CharsIsRejected() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + "a".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("size must be between 1 and 100"));
    }

    // SPEC 7.3: the name length rule applies "tras trim" (after trimming).

    @Test
    void paddedNameOf101CharsAfterTrimIsRejected() throws Exception {
        String raw = "a".repeat(101) + " ".repeat(19); // 120 chars raw, 101 after trim
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + raw + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").value("size must be between 1 and 100"));
    }

    @Test
    void paddedNameOfExactly100CharsAfterTrimIsAccepted() throws Exception {
        String raw = " ".repeat(10) + "b".repeat(100) + " ".repeat(10); // 120 chars raw, 100 after trim
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + raw + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("b".repeat(100)));
    }

    @Test
    void descriptionOf1001CharsIsRejected() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Demo\",\"description\":\"" + "d".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("description"))
                .andExpect(jsonPath("$.errors[0].message").value("size must be between 0 and 1000"));
    }

    // --- ProblemDetail contract (E-14, E-15, 405, unknown properties) -------

    @Test
    void nonNumericIdReturns400() throws Exception {
        mockMvc.perform(get("/api/projects/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid value for parameter 'id'"));
    }

    @Test
    void malformedJsonReturns400WithoutErrorsArray() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not valid json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Malformed request body"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        long id = createProject("Metodos");

        mockMvc.perform(post("/api/projects/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unknownRouteReturns404ProblemDetail() throws Exception {
        mockMvc.perform(get("/nope"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                .andExpect(jsonPath("$.detail").value(not("Unexpected error")));
    }

    @Test
    void trailingSlashOnProjectReturns404() throws Exception {
        mockMvc.perform(get("/api/projects/1/"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value(not("Unexpected error")));
    }

    @Test
    void unsupportedContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name=Demo"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.detail").value(not("Unexpected error")));
    }

    @Test
    void unknownJsonPropertiesAreIgnored() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Demo\",\"extra\":\"ignored\"}"))
                .andExpect(status().isCreated());
    }

    // --- helpers -------------------------------------------------------------

    private long createProject(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":" + "\"" + name + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Number id = JsonPath.read(body, "$.id");
        return id.longValue();
    }

    /** Inserts a project with a fixed {@code created_at} (bypasses {@code now()} default). */
    private void seedProjectWithCreatedAt(String name, Timestamp createdAt) {
        jdbcTemplate.update(
                "INSERT INTO project (name, created_at, updated_at) VALUES (?, ?, ?)",
                name, createdAt, createdAt);
    }
}
