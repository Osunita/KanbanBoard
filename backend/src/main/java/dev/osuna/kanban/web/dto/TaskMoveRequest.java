package dev.osuna.kanban.web.dto;

import dev.osuna.kanban.domain.TaskStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Body of {@code PATCH /api/tasks/{id}/move} (SPEC 7.1/7.3, D7).
 * {@code position} is an {@link Integer} (not a primitive) so that a missing
 * field fails with 400 instead of silently defaulting to 0.
 */
public record TaskMoveRequest(
        @NotNull(message = "must not be null")
        TaskStatus status,
        @NotNull(message = "must not be null")
        @Min(value = 0, message = "must be greater than or equal to 0")
        Integer position) {
}
