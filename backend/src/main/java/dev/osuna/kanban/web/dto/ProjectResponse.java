package dev.osuna.kanban.web.dto;

import dev.osuna.kanban.domain.Project;

import java.time.Instant;

/**
 * Project representation (SPEC 7.2). JPA entities never leave the service layer;
 * this record is the only shape exposed over HTTP (DEC-022).
 */
public record ProjectResponse(
        Long id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}
