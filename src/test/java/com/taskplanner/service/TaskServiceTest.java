package com.taskplanner.service;

import com.taskplanner.enums.Priority;
import com.taskplanner.enums.Status;
import com.taskplanner.model.Task;
import com.taskplanner.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TaskService}.
 * The repository is mocked — no database required.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    private Task sampleTask;

    @BeforeEach
    void setUp() {
        sampleTask = new Task();
        sampleTask.setTitle("Write unit tests");
        sampleTask.setDescription("Cover service layer");
        sampleTask.setDueDate(LocalDate.now().plusDays(7));
        sampleTask.setPriority(Priority.HIGH);
        sampleTask.setStatus(Status.TODO);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create – valid task is saved and returned")
    void create_validTask_returnsSaved() {
        Task saved = buildPersistedTask(sampleTask);
        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        Task result = taskService.create(sampleTask);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Write unit tests");
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    @DisplayName("create – blank title throws IllegalArgumentException")
    void create_blankTitle_throwsException() {
        sampleTask.setTitle("   ");

        assertThatThrownBy(() -> taskService.create(sampleTask))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Title must not be empty");

        verify(taskRepository, never()).save(any());
    }

    @Test
    @DisplayName("create – null title throws IllegalArgumentException")
    void create_nullTitle_throwsException() {
        sampleTask.setTitle(null);

        assertThatThrownBy(() -> taskService.create(sampleTask))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("create – null due date throws IllegalArgumentException")
    void create_nullDueDate_throwsException() {
        sampleTask.setDueDate(null);

        assertThatThrownBy(() -> taskService.create(sampleTask))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Due date is required");
    }

    @Test
    @DisplayName("create – past due date is accepted (no PastDate restriction)")
    void create_pastDueDate_isAccepted() {
        sampleTask.setDueDate(LocalDate.of(2020, 1, 1));
        Task saved = buildPersistedTask(sampleTask);
        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        assertThatCode(() -> taskService.create(sampleTask)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("create – null priority throws IllegalArgumentException")
    void create_nullPriority_throwsException() {
        sampleTask.setPriority(null);

        assertThatThrownBy(() -> taskService.create(sampleTask))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Priority is required");
    }

    @Test
    @DisplayName("create – null status throws IllegalArgumentException")
    void create_nullStatus_throwsException() {
        sampleTask.setStatus(null);

        assertThatThrownBy(() -> taskService.create(sampleTask))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Status is required");
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById – existing id returns task")
    void findById_existingId_returnsTask() {
        Task saved = buildPersistedTask(sampleTask);
        when(taskRepository.findById(saved.getId())).thenReturn(Optional.of(saved));

        Task result = taskService.findById(saved.getId());

        assertThat(result).isEqualTo(saved);
    }

    @Test
    @DisplayName("findById – unknown id throws NoSuchElementException")
    void findById_unknownId_throwsException() {
        UUID random = UUID.randomUUID();
        when(taskRepository.findById(random)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.findById(random))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining(random.toString());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update – changes are persisted")
    void update_validData_persistsChanges() {
        Task existing = buildPersistedTask(sampleTask);
        when(taskRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenReturn(existing);

        Task updated = new Task();
        updated.setTitle("Updated title");
        updated.setDueDate(LocalDate.now().plusDays(14));
        updated.setPriority(Priority.LOW);
        updated.setStatus(Status.IN_PROGRESS);

        taskService.update(existing.getId(), updated);

        assertThat(existing.getTitle()).isEqualTo("Updated title");
        assertThat(existing.getStatus()).isEqualTo(Status.IN_PROGRESS);
        verify(taskRepository).save(existing);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete – calls repository delete once")
    void delete_existingId_deletesTask() {
        Task saved = buildPersistedTask(sampleTask);
        when(taskRepository.findById(saved.getId())).thenReturn(Optional.of(saved));

        taskService.delete(saved.getId());

        verify(taskRepository, times(1)).delete(saved);
    }

    // ── getStats ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStats – returns map with all four keys")
    void getStats_returnsCorrectKeys() {
        when(taskRepository.countByStatus(Status.TODO)).thenReturn(3L);
        when(taskRepository.countByStatus(Status.IN_PROGRESS)).thenReturn(1L);
        when(taskRepository.countByStatus(Status.DONE)).thenReturn(5L);
        when(taskRepository.countOverdue(any(LocalDate.class))).thenReturn(2L);

        var stats = taskService.getStats();

        assertThat(stats).containsKeys("todo", "inProgress", "done", "overdue");
        assertThat(stats.get("todo")).isEqualTo(3L);
        assertThat(stats.get("overdue")).isEqualTo(2L);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Task buildPersistedTask(Task source) {
        Task t = new Task();
        t.setTitle(source.getTitle());
        t.setDescription(source.getDescription());
        t.setDueDate(source.getDueDate());
        t.setPriority(source.getPriority());
        t.setStatus(source.getStatus() != null ? source.getStatus() : Status.TODO);

        // Simulate @PrePersist
        try {
            var id = Task.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(t, UUID.randomUUID());
            var created = Task.class.getDeclaredField("createdAt");
            created.setAccessible(true);
            created.set(t, LocalDateTime.now());
            var updated = Task.class.getDeclaredField("updatedAt");
            updated.setAccessible(true);
            updated.set(t, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return t;
    }
}
