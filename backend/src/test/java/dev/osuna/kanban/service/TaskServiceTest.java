package dev.osuna.kanban.service;

import dev.osuna.kanban.domain.Project;
import dev.osuna.kanban.domain.Task;
import dev.osuna.kanban.domain.TaskStatus;
import dev.osuna.kanban.repository.ProjectRepository;
import dev.osuna.kanban.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link TaskService} — TEST-015…017 (SPEC 12.2).
 *
 * <p>The repository mock is backed by a stateful in-memory store instead of
 * fixed returns (design D2/S6 rationale): a create/move/delete sequence only
 * stays coherent if every query reflects the mutations, so assertions target
 * position values and the exact exception message rather than brittle call
 * counts. The store filters by each task's live status, which emulates the
 * auto-flush exclusion the real EntityManager performs when {@code move()} and
 * {@code delete()} query a column after mutating it (batch-1 apply notes).</p>
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    private static final Long PROJECT_ID = 1L;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private TaskService taskService;

    private final Project project = new Project("Demo", null);
    private final Map<Long, Task> tasksById = new LinkedHashMap<>();
    private long nextTaskId = 1L;

    @BeforeEach
    void stubStatefulRepository() {
        // Lenient: the shared store serves every test, but each test only uses
        // part of it — strict stubbing would flag the unused branches.
        lenient().when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        lenient().when(taskRepository.findById(any()))
                .thenAnswer(invocation -> Optional.ofNullable(tasksById.get(invocation.<Long>getArgument(0))));
        lenient().when(taskRepository.save(any(Task.class)))
                .thenAnswer(invocation -> store(invocation.getArgument(0)));
        lenient().when(taskRepository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<Task> entities = invocation.getArgument(0);
            entities.forEach(this::store);
            return entities;
        });
        lenient().when(taskRepository.findByProjectIdAndStatusOrderByPositionAsc(any(), any()))
                .thenAnswer(invocation -> column(invocation.getArgument(1)));
        lenient().when(taskRepository.findByProjectId(any()))
                .thenAnswer(invocation -> new ArrayList<>(tasksById.values()));
        lenient().when(taskRepository.countByProjectIdAndStatus(any(), any()))
                .thenAnswer(invocation -> (long) column(invocation.getArgument(1)).size());
        lenient().doAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            tasksById.remove(task.getId());
            return null;
        }).when(taskRepository).delete(any(Task.class));
        // flush() keeps the default no-op: the store is never out of sync, which
        // is exactly the post-flush state the real repository would expose.
    }

    // --- TEST-015: same-column moves ----------------------------------------

    @Test
    void moveToSameColumnAndPositionLeavesEveryPositionUnchanged() {
        Task a = create("A");
        create("B");
        create("C");

        Task result = taskService.move(a.getId(), TaskStatus.TODO, 0);

        assertThat(result).isSameAs(a);
        assertThat(column(TaskStatus.TODO)).extracting(Task::getTitle).containsExactly("A", "B", "C");
        assertThat(column(TaskStatus.TODO)).extracting(Task::getPosition).containsExactly(0, 1, 2);
    }

    @Test
    void moveWithinSameColumnRenumbersAroundTheMovedTask() {
        Task a = create("A");
        create("B");
        create("C");

        taskService.move(a.getId(), TaskStatus.TODO, 2);

        assertThat(column(TaskStatus.TODO)).extracting(Task::getTitle).containsExactly("B", "C", "A");
        assertThat(column(TaskStatus.TODO)).extracting(Task::getPosition).containsExactly(0, 1, 2);
    }

    // --- TEST-016: out-of-range move ----------------------------------------

    @Test
    void moveBeyondColumnSizeThrowsAndSavesNothing() {
        Task a = create("A");
        create("B");
        create("C");

        assertThatThrownBy(() -> taskService.move(a.getId(), TaskStatus.IN_PROGRESS, 5))
                .isInstanceOf(InvalidPositionException.class)
                .hasMessage("Position 5 is out of range for column IN_PROGRESS (max 0)");

        verify(taskRepository, never()).saveAll(any());
        assertThat(column(TaskStatus.TODO)).extracting(Task::getTitle).containsExactly("A", "B", "C");
        assertThat(column(TaskStatus.TODO)).extracting(Task::getPosition).containsExactly(0, 1, 2);
        assertThat(column(TaskStatus.IN_PROGRESS)).isEmpty();
    }

    // --- TEST-017: invariant under arbitrary sequences ----------------------

    @Test
    void positionsStayContiguousAcrossArbitraryCreateMoveDeleteSequences() {
        create("seed-0");
        create("seed-1");

        Random random = new Random(42);
        for (int i = 0; i < 200; i++) {
            int roll = random.nextInt(10);
            if (roll < 4) {
                taskService.create(PROJECT_ID, "T" + i, null, null);
            } else if (roll < 7) {
                Task victim = pickRandomTask(random);
                if (victim != null) {
                    taskService.delete(victim.getId());
                }
            } else {
                Task victim = pickRandomTask(random);
                if (victim != null) {
                    TaskStatus target = TaskStatus.values()[random.nextInt(TaskStatus.values().length)];
                    try {
                        // Positions 0..4 — deliberately includes out-of-range requests.
                        taskService.move(victim.getId(), target, random.nextInt(5));
                    } catch (InvalidPositionException outOfRange) {
                        // Legitimate 400 (E-08): nothing persisted, invariant must still hold.
                        assertThat(outOfRange).hasMessageContaining("is out of range for column");
                    }
                }
            }
            assertEveryColumnIsContiguousZeroToNMinusOne();
        }
    }

    // --- helpers ------------------------------------------------------------

    private Task create(String title) {
        return taskService.create(PROJECT_ID, title, null, null);
    }

    private Task pickRandomTask(Random random) {
        List<Task> tasks = List.copyOf(tasksById.values());
        return tasks.isEmpty() ? null : tasks.get(random.nextInt(tasks.size()));
    }

    /** Live column of the store: matches by current status, ordered by position (post-flush view). */
    private List<Task> column(TaskStatus status) {
        List<Task> column = new ArrayList<>();
        tasksById.values().stream()
                .filter(task -> task.getStatus() == status)
                .sorted(Comparator.comparingInt(Task::getPosition))
                .forEach(column::add);
        return column;
    }

    /** Identity-based assertion of SPEC 6.1: positions are exactly 0, 1, … n-1 per column. */
    private void assertEveryColumnIsContiguousZeroToNMinusOne() {
        for (TaskStatus status : TaskStatus.values()) {
            List<Integer> positions = column(status).stream().map(Task::getPosition).toList();
            assertThat(positions)
                    .describedAs("positions of column %s", status)
                    .containsExactlyElementsOf(IntStream.range(0, positions.size()).boxed().toList());
        }
    }

    private Task store(Task task) {
        if (task.getId() == null) {
            setId(task, nextTaskId++);
        }
        tasksById.put(task.getId(), task);
        return task;
    }

    /** Assigns a synthetic id — entities use identity generation, so there is no setter (test-only). */
    private static void setId(Task task, long id) {
        try {
            Field field = Task.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(task, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot assign a test id to Task", e);
        }
    }
}
