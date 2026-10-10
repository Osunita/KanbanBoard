package dev.osuna.kanban.web.dto;

import dev.osuna.kanban.web.validation.TrimmedSize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/projects} and {@code PUT /api/projects/{id}}
 * (same shape, SPEC 7.1/7.3). Unknown JSON properties are ignored.
 * The name length rule applies after trimming (SPEC 7.3 "tras trim").
 */
public record ProjectRequest(
        @NotBlank(message = "must not be blank")
        @TrimmedSize(max = 100, message = "size must be between 1 and 100")
        String name,
        @Size(max = 1000, message = "size must be between 0 and 1000")
        String description) {
}
