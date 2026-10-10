package dev.osuna.kanban.web.dto;

import dev.osuna.kanban.domain.TaskPriority;
import dev.osuna.kanban.web.validation.TrimmedSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PUT /api/tasks/{id}} (SPEC 7.1/7.3): replaces title, description
 * and priority only — status and position are not part of this shape. Priority is
 * required here (SPEC 7.3 "Task.priority (PUT) obligatorio").
 */
public record TaskUpdateRequest(
        @NotBlank(message = "must not be blank")
        @TrimmedSize(max = 200, message = "size must be between 1 and 200")
        String title,
        @Size(max = 2000, message = "size must be between 0 and 2000")
        String description,
        @NotNull(message = "must not be null")
        TaskPriority priority) {
}
