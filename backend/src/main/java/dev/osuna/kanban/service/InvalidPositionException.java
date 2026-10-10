package dev.osuna.kanban.service;

import dev.osuna.kanban.domain.TaskStatus;

/**
 * Thrown when a move requests a position outside the target column; mapped to
 * HTTP 400 by the {@code GlobalExceptionHandler} (SPEC 7.4 step 3, 7.5).
 * The message is formatted exactly once here so the API detail and the tests
 * share a single source of truth (D5).
 */
public class InvalidPositionException extends RuntimeException {

    public InvalidPositionException(int position, TaskStatus column, int max) {
        super(String.format("Position %d is out of range for column %s (max %d)",
                position, column.name(), max));
    }
}
