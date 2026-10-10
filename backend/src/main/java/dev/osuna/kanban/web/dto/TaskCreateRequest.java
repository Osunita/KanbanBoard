package dev.osuna.kanban.web.dto;

import dev.osuna.kanban.domain.TaskPriority;
import dev.osuna.kanban.web.validation.TrimmedSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/projects/{projectId}/tasks} (SPEC 7.1/7.3).
 * Unknown JSON properties are ignored. The title length rule applies after
 * trimming (SPEC 7.3 "tras trim"); priority is optional and defaults to
 * {@code MEDIUM} in the service (SPEC 7.1).
 */
public record TaskCreateRequest(
        @NotBlank(message = "must not be blank")
        @TrimmedSize(max = 200, message = "size must be between 1 and 200")
        String title,
        @Size(max = 2000, message = "size must be between 0 and 2000")
        String description,
        TaskPriority priority) {
}
