package dev.osuna.kanban.service;

import dev.osuna.kanban.domain.Project;
import dev.osuna.kanban.repository.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProjectService} with a mocked repository (SPEC 12.1).
 */
@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void createSavesTrimmedNameAndDescription() {
        when(projectRepository.save(org.mockito.ArgumentMatchers.any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Project created = projectService.create("  Demo  ", "Some description");

        assertThat(created.getName()).isEqualTo("Demo");
        assertThat(created.getDescription()).isEqualTo("Some description");
        verify(projectRepository).save(created);
    }

    @Test
    void createKeepsNullDescription() {
        when(projectRepository.save(org.mockito.ArgumentMatchers.any(Project.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Project created = projectService.create("Demo", null);

        assertThat(created.getName()).isEqualTo("Demo");
        assertThat(created.getDescription()).isNull();
    }

    @Test
    void findAllDelegatesToOrderedRepositoryQuery() {
        List<Project> projects = List.of(new Project("B", null), new Project("A", null));
        when(projectRepository.findAllByOrderByCreatedAtDesc()).thenReturn(projects);

        assertThat(projectService.findAll()).isEqualTo(projects);
    }

    @Test
    void getByIdReturnsExistingProject() {
        Project project = new Project("Demo", null);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThat(projectService.getById(1L)).isSameAs(project);
    }

    @Test
    void getByIdThrowsNotFoundWithExpectedMessage() {
        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project 999 not found");
    }

    @Test
    void updateReplacesNameAndDescription() {
        Project project = new Project("Old", "old description");
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        Project updated = projectService.update(1L, "  New  ", "new description");

        assertThat(updated.getName()).isEqualTo("New");
        assertThat(updated.getDescription()).isEqualTo("new description");
    }

    @Test
    void updateWithNullDescriptionClearsIt() {
        Project project = new Project("Old", "old description");
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        Project updated = projectService.update(1L, "New", null);

        assertThat(updated.getName()).isEqualTo("New");
        assertThat(updated.getDescription()).isNull();
    }

    @Test
    void updateThrowsNotFoundForMissingProject() {
        when(projectRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.update(404L, "Name", null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project 404 not found");
    }

    @Test
    void deleteRemovesExistingProject() {
        Project project = new Project("Demo", null);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        projectService.delete(1L);

        verify(projectRepository).delete(project);
    }

    @Test
    void deleteThrowsNotFoundForMissingProject() {
        when(projectRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.delete(7L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project 7 not found");
    }
}
