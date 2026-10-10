package dev.osuna.kanban.service;

import dev.osuna.kanban.domain.Project;
import dev.osuna.kanban.domain.Task;
import dev.osuna.kanban.domain.TaskPriority;
import dev.osuna.kanban.domain.TaskStatus;
import dev.osuna.kanban.repository.ProjectRepository;
import dev.osuna.kanban.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Transactional CRUD for tasks plus the board's position invariant (SPEC 5.1/6.1):
 * for every (project, status) pair the positions are exactly 0..n-1 after every
 * create, move and delete. All business rules live here; HTTP concerns stay in
 * the {@code web} layer.
 */
@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;

    public TaskService(TaskRepository taskRepository, ProjectRepository projectRepository) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
    }

    /**
     * Creates a task at the end of the TODO column with priority defaulting to
     * MEDIUM (SPEC 7.1). 404 when the project does not exist (nothing created).
     */
    @Transactional
    public Task create(Long projectId, String title, String description, TaskPriority priority) {
        Project project = findProject(projectId);
        long position = taskRepository.countByProjectIdAndStatus(projectId, TaskStatus.TODO);
        TaskPriority effectivePriority = priority != null ? priority : TaskPriority.MEDIUM;
        return taskRepository.save(new Task(project, title.trim(), description, effectivePriority, (int) position));
    }

    /**
     * All tasks of a project ordered by column (enum order TODO, IN_PROGRESS, DONE)
     * then position (SPEC 6.3, 7.1). 404 when the project does not exist (E-03).
     */
    @Transactional(readOnly = true)
    public List<Task> findAllForProject(Long projectId) {
        findProject(projectId);
        return taskRepository.findByProjectId(projectId).stream()
                .sorted(Comparator.comparingInt((Task task) -> task.getStatus().ordinal())
                        .thenComparingInt(Task::getPosition))
                .toList();
    }

    @Transactional(readOnly = true)
    public Task getById(Long id) {
        return findTask(id);
    }

    /**
     * PUT semantics: replaces title, description and priority only — status and
     * position are untouched (SPEC 7.1).
     */
    @Transactional
    public Task update(Long id, String title, String description, TaskPriority priority) {
        Task task = findTask(id);
        task.setTitle(title.trim());
        task.setDescription(description);
        task.setPriority(priority);
        return task;
    }

    /**
     * Deletes a task and renumbers its source column back to 0..n-1 (E-12).
     * The column is queried after the pending delete has been flushed so the
     * deleted entity is never saved again (D2).
     */
    @Transactional
    public void delete(Long id) {
        Task task = findTask(id);
        Long projectId = task.getProject().getId();
        TaskStatus source = task.getStatus();
        taskRepository.delete(task);
        taskRepository.flush();
        List<Task> sourceColumn = taskRepository.findByProjectIdAndStatusOrderByPositionAsc(projectId, source);
        renumber(sourceColumn);
        taskRepository.saveAll(sourceColumn);
    }

    /**
     * Move per SPEC 7.4, executed as a single transaction (D1):
     * build the destination list without the task, range-check it, splice the
     * task in, renumber the destination and (on a status change) the source column.
     * A move to the current status and position leaves every value unchanged, so
     * dirty checking persists nothing (D4, E-09).
     */
    @Transactional
    public Task move(Long id, TaskStatus status, int position) {
        Task task = findTask(id);
        Long projectId = task.getProject().getId();
        TaskStatus source = task.getStatus();

        List<Task> destination = taskRepository.findByProjectIdAndStatusOrderByPositionAsc(projectId, status);
        destination.remove(task);
        if (position > destination.size()) {
            throw new InvalidPositionException(position, status, destination.size());
        }
        destination.add(position, task);
        task.setStatus(status);
        renumber(destination);
        taskRepository.saveAll(destination);

        if (status != source) {
            // The flush performed by the query below already excludes the moved task.
            List<Task> sourceColumn = taskRepository.findByProjectIdAndStatusOrderByPositionAsc(projectId, source);
            renumber(sourceColumn);
            taskRepository.saveAll(sourceColumn);
        }
        return task;
    }

    private void renumber(List<Task> column) {
        for (int i = 0; i < column.size(); i++) {
            column.get(i).setPosition(i);
        }
    }

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task " + id + " not found"));
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + id + " not found"));
    }
}
