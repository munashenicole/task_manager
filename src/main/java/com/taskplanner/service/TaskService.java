package com.taskplanner.service;

import com.taskplanner.enums.Priority;
import com.taskplanner.enums.Status;
import com.taskplanner.model.Task;
import com.taskplanner.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;

    // ── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Task> findAll(String titleSearch, Status status, Priority priority) {
        String search = (titleSearch != null && !titleSearch.isBlank()) ? titleSearch.trim() : null;
        log.debug("findAll – title='{}', status={}, priority={}", search, status, priority);
        return taskRepository.findWithFilters(search, status, priority);
    }

    @Transactional(readOnly = true)
    public Task findById(UUID id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Task not found: " + id));
    }

    // ── Write ─────────────────────────────────────────────────────────────────

    @Transactional
    public Task create(Task task) {
        validate(task);
        Task saved = taskRepository.save(task);
        log.info("Created task id={} title='{}'", saved.getId(), saved.getTitle());
        return saved;
    }

    @Transactional
    public Task update(UUID id, Task updated) {
        Task existing = findById(id);
        validate(updated);

        existing.setTitle(updated.getTitle().trim());
        existing.setDescription(updated.getDescription());
        existing.setDueDate(updated.getDueDate());
        existing.setPriority(updated.getPriority());
        existing.setStatus(updated.getStatus());

        Task saved = taskRepository.save(existing);
        log.info("Updated task id={}", saved.getId());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        Task task = findById(id);
        taskRepository.delete(task);
        log.info("Deleted task id={}", id);
    }

    // ── Stats ──────────────────────────────────────────────────────────────────

    /**
     * Returns a summary map used by the dashboard:
     * <pre>
     *   todo       – count of TODO tasks
     *   inProgress – count of IN_PROGRESS tasks
     *   done       – count of DONE tasks
     *   overdue    – count of tasks whose due date is before today and not Done
     * </pre>
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getStats() {
        return Map.of(
                "todo",       taskRepository.countByStatus(Status.TODO),
                "inProgress", taskRepository.countByStatus(Status.IN_PROGRESS),
                "done",       taskRepository.countByStatus(Status.DONE),
                "overdue",    taskRepository.countOverdue(LocalDate.now())
        );
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private void validate(Task task) {
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new IllegalArgumentException("Title must not be empty");
        }
        if (task.getDueDate() == null) {
            throw new IllegalArgumentException("Due date is required");
        }
        if (task.getPriority() == null) {
            throw new IllegalArgumentException("Priority is required");
        }
        if (task.getStatus() == null) {
            throw new IllegalArgumentException("Status is required");
        }
    }
}
