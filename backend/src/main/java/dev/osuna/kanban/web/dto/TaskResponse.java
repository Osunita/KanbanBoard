package dev.osuna.kanban.web.dto;

import dev.osuna.kanban.domain.Task;
import dev.osuna.kanban.domain.TaskPriority;
import dev.osuna.kanban.domain.TaskStatus;

import java.time.Instant;

/**
 * Task representation (SPEC 7.2). JPA entities never leave the service layer;
 * this record is the only shape exposed over HTTP (DEC-022).
 * {@code projectId} comes from the lazy association: reading only the id of the
 * Hibernate proxy does not initialize it, so it is safe with open-in-view=false (D8).
 */
public record TaskResponse(
        Long id,
        Long projectId,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        int position,
        Instant createdAt,
        Instant updatedAt) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getProject().getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getPosition(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
