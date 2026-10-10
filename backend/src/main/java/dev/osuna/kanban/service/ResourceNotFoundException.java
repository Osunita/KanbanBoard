package dev.osuna.kanban.service;

/**
 * Thrown when a requested resource does not exist; mapped to HTTP 404 by the
 * {@code GlobalExceptionHandler} (SPEC 7.5).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
