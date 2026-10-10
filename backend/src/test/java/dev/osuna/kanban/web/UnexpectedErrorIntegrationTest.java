package dev.osuna.kanban.web;

import dev.osuna.kanban.service.ProjectService;
import dev.osuna.kanban.support.PostgresContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPEC 11 / E-17: when an unexpected error occurs, the response is a 500 ProblemDetail
 * with only {@code detail: Unexpected error} — no stack trace, class names or internal
 * messages in the body (the trace goes to the server log only).
 * <p>{@code @MockitoBean} (Spring Framework 7 — Boot 4 removed {@code @MockBean}) replaces
 * {@link ProjectService} so {@code GET /api/projects} fails on purpose. The mock lives in
 * its own test class so the other integration tests keep the real service.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresContainerConfig.class)
class UnexpectedErrorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    @Test
    void unexpectedErrorReturns500WithoutLeakingInternals() throws Exception {
        when(projectService.findAll())
                .thenThrow(new RuntimeException("internal detail that must not leak"));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.detail").value("Unexpected error"))
                .andExpect(jsonPath("$.errors").doesNotExist())
                // No internals in the body: no exception class names, no messages, no frames.
                .andExpect(content().string(not(containsString("RuntimeException"))))
                .andExpect(content().string(not(containsString("must not leak"))))
                .andExpect(content().string(not(containsString("stackTrace"))))
                .andExpect(content().string(not(containsString("dev.osuna"))));
    }
}
