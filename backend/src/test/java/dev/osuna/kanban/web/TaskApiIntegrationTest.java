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
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the task API against real PostgreSQL (SPEC 12.1/12.2):
 * TEST-005…014 plus the task E-cases (E-03 create/list in a missing project,
 * E-10/E-11 empty target column) and the carried {@code GET …/tasks → 404}
 * assertion that completes TEST-004. Docker is required; each test starts from
 * a clean database (the cascade in {@code @BeforeEach} clears tasks too).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
class TaskApiIntegrationTest {

    private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        // Cascade removes every task of the deleted projects.
        jdbcTemplate.update("DELETE FROM project");
    }

    // --- TEST-005: sequential creation fills positions ----------------------

    @Test
    void createThreeTasksFillsTodoPositions() throws Exception {
        long projectId = createProject("Tablero");
        long a = createTask(projectId, "A");
        long b = createTask(projectId, "B");
        long c = createTask(projectId, "C");

        mockMvc.perform(get("/api/projects/" + projectId + "/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(a))
                .andExpect(jsonPath("$[0].projectId").value(projectId))
                .andExpect(jsonPath("$[0].title").value("A"))
                .andExpect(jsonPath("$[0].status").value("TODO"))
                .andExpect(jsonPath("$[0].priority").value("MEDIUM"))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$[0].updatedAt").isNotEmpty())
                .andExpect(jsonPath("$[1].id").value(b))
                .andExpect(jsonPath("$[1].status").value("TODO"))
                .andExpect(jsonPath("$[1].priority").value("MEDIUM"))
                .andExpect(jsonPath("$[1].position").value(1))
                .andExpect(jsonPath("$[2].id").value(c))
                .andExpect(jsonPath("$[2].status").value("TODO"))
                .andExpect(jsonPath("$[2].position").value(2));
    }

    // --- TEST-006: move across columns --------------------------------------

    @Test
    void moveAcrossColumnsRenumbersBothColumns() throws Exception {
        long projectId = createProject("Tablero");
        long a = createTask(projectId, "A");
        createTask(projectId, "B");
        createTask(projectId, "C");

        mockMvc.perform(patch("/api/tasks/" + a + "/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"position\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.position").value(0));

        mockMvc.perform(get("/api/projects/" + projectId + "/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("B"))
                .andExpect(jsonPath("$[0].status").value("TODO"))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[1].title").value("C"))
                .andExpect(jsonPath("$[1].status").value("TODO"))
                .andExpect(jsonPath("$[1].position").value(1))
                .andExpect(jsonPath("$[2].title").value("A"))
                .andExpect(jsonPath("$[2].status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[2].position").value(0));
    }

    // --- TEST-007: move downward in the same column -------------------------

    @Test
    void moveDownwardWithinSameColumnRenumbers() throws Exception {
        long projectId = createProject("Tablero");
        long a = createTask(projectId, "A");
        createTask(projectId, "B");
        createTask(projectId, "C");

        mockMvc.perform(patch("/api/tasks/" + a + "/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TODO\",\"position\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.position").value(2));

        mockMvc.perform(get("/api/projects/" + projectId + "/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("B"))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[1].title").value("C"))
                .andExpect(jsonPath("$[1].position").value(1))
                .andExpect(jsonPath("$[2].title").value("A"))
                .andExpect(jsonPath("$[2].position").value(2));
    }

    // --- TEST-008: move upward in the same column ---------------------------

    @Test
    void moveUpwardWithinSameColumnRenumbers() throws Exception {
        long projectId = createProject("Tablero");
        createTask(projectId, "A");
        createTask(projectId, "B");
        long c = createTask(projectId, "C");

        mockMvc.perform(patch("/api/tasks/" + c + "/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"TODO\",\"position\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(0));

        mockMvc.perform(get("/api/projects/" + projectId + "/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].title").value("C"))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[1].title").value("A"))
                .andExpect(jsonPath("$[1].position").value(1))
                .andExpect(jsonPath("$[2].title").value("B"))
                .andExpect(jsonPath("$[2].position").value(2));
    }

    // --- TEST-009 / E-08: out-of-range position persists nothing ------------

    @Test
    void outOfRangeMoveReturns400AndPersistsNothing() throws Exception {
        long projectId = createProject("Tablero");
        long a = createTask(projectId, "A");
        createTask(projectId, "B");

        mockMvc.perform(patch("/api/tasks/" + a + "/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\",\"position\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail")
                        .value("Position 1 is out of range for column IN_PROGRESS (max 0)"));

        mockMvc.perform(get("/api/projects/" + projectId + "/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("A"))
                .andExpect(jsonPath("$[0].status").value("TODO"))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[1].title").value("B"))
                .andExpect(jsonPath("$[1].status").value("TODO"))
                .andExpect(jsonPath("$[1].position").value(1));
    }

    // --- TEST-010 / E-12: deleting the middle task renumbers ----------------

    @Test
    void deleteMiddleTaskRenumbersSourceColumn() throws Exception {
        long projectId = createProject("Tablero");
        long a = createTask(projectId, "A");
        long b = createTask(projectId, "B");
        long c = createTask(projectId, "C");

        mockMvc.perform(delete("/api/tasks/" + b))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/" + projectId + "/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(a))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[1].id").value(c))
                .andExpect(jsonPath("$[1].position").value(1));
    }

    // --- TEST-011: PUT replaces three fields only ---------------------------

    @Test
    void putReplacesTitleAndPriorityKeepingStatusAndPosition() throws Exception {
        long projectId = createProject("Tablero");
        createTask(projectId, "Primera");
        long second = createTaskBody(projectId, "{\"title\":\"Segunda\",\"priority\":\"HIGH\"}");

        MvcResult result = mockMvc.perform(put("/api/tasks/" + second)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Nuevo\",\"priority\":\"LOW\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Nuevo"))
                .andExpect(jsonPath("$.priority").value("LOW"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.position").value(1))
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String createdAt = JsonPath.read(body, "$.createdAt");
        String updatedAt = JsonPath.read(body, "$.updatedAt");
        assertThat(Instant.parse(updatedAt).toEpochMilli())
                .isGreaterThanOrEqualTo(Instant.parse(createdAt).toEpochMilli());
    }

    // --- TEST-012 / E-06: unknown enum value --------------------------------

    @Test
    void unknownMoveStatusReturns400MalformedBody() throws Exception {
        mockMvc.perform(patch("/api/tasks/1/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLOCKED\",\"position\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.detail").value("Malformed request body"));
    }

    // --- TEST-013 / E-14: non-numeric path id -------------------------------

    @Test
    void nonNumericTaskIdReturns400() throws Exception {
        mockMvc.perform(get("/api/tasks/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Invalid value for parameter 'id'"));
    }

    // --- TEST-014 / E-05: title length boundaries ---------------------------

    @Test
    void titleOfExactly200CharsIsAccepted() throws Exception {
        long projectId = createProject("Tablero");

        mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "a".repeat(200) + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/tasks/\\d+")))
                .andExpect(jsonPath("$.title").value("a".repeat(200)))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void titleOf201CharsIsRejected() throws Exception {
        long projectId = createProject("Tablero");

        mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "a".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("title"))
                .andExpect(jsonPath("$.errors[0].message").value("size must be between 1 and 200"));
    }

    // --- E-03: missing project (create and list) ----------------------------

    @Test
    void createTaskInMissingProjectReturns404() throws Exception {
        mockMvc.perform(post("/api/projects/999/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Project 999 not found"));

        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM task WHERE project_id = ?", Integer.class, 999L);
        assertThat(remaining).isZero();
    }

    @Test
    void listTasksOfMissingProjectReturns404() throws Exception {
        mockMvc.perform(get("/api/projects/999/tasks"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.detail").value("Project 999 not found"));
    }

    // --- E-10 / E-11: empty target column -----------------------------------

    @Test
    void moveIntoEmptyColumnAtPositionZeroRenumbersSource() throws Exception {
        long projectId = createProject("Tablero");
        long a = createTask(projectId, "A");
        createTask(projectId, "B");

        mockMvc.perform(patch("/api/tasks/" + a + "/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\",\"position\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.position").value(0));

        mockMvc.perform(get("/api/projects/" + projectId + "/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("B"))
                .andExpect(jsonPath("$[0].status").value("TODO"))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[1].title").value("A"))
                .andExpect(jsonPath("$[1].status").value("DONE"))
                .andExpect(jsonPath("$[1].position").value(0));
    }

    @Test
    void moveIntoEmptyColumnAtPositionOneReturns400() throws Exception {
        long projectId = createProject("Tablero");
        long a = createTask(projectId, "A");
        createTask(projectId, "B");

        mockMvc.perform(patch("/api/tasks/" + a + "/move")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\",\"position\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.detail")
                        .value("Position 1 is out of range for column DONE (max 0)"));

        mockMvc.perform(get("/api/projects/" + projectId + "/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("A"))
                .andExpect(jsonPath("$[0].status").value("TODO"))
                .andExpect(jsonPath("$[0].position").value(0))
                .andExpect(jsonPath("$[1].status").value("TODO"))
                .andExpect(jsonPath("$[1].position").value(1));
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

    private long createTask(long projectId, String title) throws Exception {
        return createTaskBody(projectId, "{\"title\":\"" + title + "\"}");
    }

    private long createTaskBody(long projectId, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects/" + projectId + "/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String response = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        Number id = JsonPath.read(response, "$.id");
        return id.longValue();
    }
}
