package dev.osuna.kanban.service;

import dev.osuna.kanban.domain.Project;
import dev.osuna.kanban.repository.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Transactional CRUD for projects (SPEC 5.1). All business rules live here;
 * HTTP concerns stay in the {@code web} layer.
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public List<Project> findAll() {
        return projectRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Project create(String name, String description) {
        return projectRepository.save(new Project(name.trim(), description));
    }

    @Transactional(readOnly = true)
    public Project getById(Long id) {
        return findProject(id);
    }

    /**
     * PUT semantics: replaces name and description (an omitted description clears it).
     */
    @Transactional
    public Project update(Long id, String name, String description) {
        Project project = findProject(id);
        project.setName(name.trim());
        project.setDescription(description);
        return project;
    }

    @Transactional
    public void delete(Long id) {
        Project project = findProject(id);
        projectRepository.delete(project);
        // Tasks are removed by the FK ON DELETE CASCADE of V1__init.sql (SPEC 6.2, DEC-019).
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + id + " not found"));
    }
}
