package dev.osuna.kanban.web;

import dev.osuna.kanban.service.TaskService;
import dev.osuna.kanban.web.dto.TaskCreateRequest;
import dev.osuna.kanban.web.dto.TaskMoveRequest;
import dev.osuna.kanban.web.dto.TaskResponse;
import dev.osuna.kanban.web.dto.TaskUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Thin controller for all six task endpoints of SPEC 7.1, including the nested
 * routes under a project (D9): routing, validation, status codes and DTO
 * conversion only — no business rules.
 */
@RestController
@RequestMapping("/api")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/projects/{projectId}/tasks")
    public List<TaskResponse> listByProject(@PathVariable Long projectId) {
        return taskService.findAllForProject(projectId).stream().map(TaskResponse::from).toList();
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> create(@PathVariable Long projectId,
                                               @Valid @RequestBody TaskCreateRequest request) {
        TaskResponse created = TaskResponse.from(taskService.create(
                projectId, request.title(), request.description(), request.priority()));
        return ResponseEntity.created(URI.create("/api/tasks/" + created.id())).body(created);
    }

    @GetMapping("/tasks/{id}")
    public TaskResponse get(@PathVariable Long id) {
        return TaskResponse.from(taskService.getById(id));
    }

    @PutMapping("/tasks/{id}")
    public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        return TaskResponse.from(taskService.update(
                id, request.title(), request.description(), request.priority()));
    }

    @PatchMapping("/tasks/{id}/move")
    public TaskResponse move(@PathVariable Long id, @Valid @RequestBody TaskMoveRequest request) {
        return TaskResponse.from(taskService.move(id, request.status(), request.position()));
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
