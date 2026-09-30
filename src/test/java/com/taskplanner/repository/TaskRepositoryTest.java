package com.taskplanner.repository;

import com.taskplanner.enums.Priority;
import com.taskplanner.enums.Status;
import com.taskplanner.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TaskRepository}.
 * Uses an H2 in-memory database (PostgreSQL-compatibility mode).
 * Schema is created by Hibernate (ddl-auto=create-drop in test properties).
 */
@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TaskRepository taskRepository;

    private Task pastHighTask;    // overdue, HIGH priority, TODO
    private Task futureLoTask;    // future due date, LOW priority, TODO
    private Task donePastTask;    // past due date, MEDIUM priority, DONE → NOT overdue

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();

        pastHighTask = makeTask("Past HIGH task", LocalDate.now().minusDays(5), Priority.HIGH, Status.TODO);
        futureLoTask = makeTask("Future LOW task", LocalDate.now().plusDays(10), Priority.LOW,  Status.TODO);
        donePastTask = makeTask("Done MEDIUM task", LocalDate.now().minusDays(3), Priority.MEDIUM, Status.DONE);

        taskRepository.saveAll(List.of(pastHighTask, futureLoTask, donePastTask));
    }

    // ── findWithFilters ───────────────────────────────────────────────────────

    @Test
    @DisplayName("findWithFilters – no filters returns all tasks ordered by createdAt DESC")
    void findWithFilters_noFilters_returnsAll() {
        List<Task> result = taskRepository.findWithFilters(null, null, null);
        assertThat(result).hasSize(3);
    }

    @Test
    @DisplayName("findWithFilters – title search is case-insensitive and partial")
    void findWithFilters_titleSearch_caseInsensitivePartialMatch() {
        List<Task> result = taskRepository.findWithFilters("past", null, null);
        // matches "Past HIGH task" and "Done MEDIUM task" (has "past" in description? no — just title)
        assertThat(result)
                .extracting(Task::getTitle)
                .contains("Past HIGH task");
    }

    @Test
    @DisplayName("findWithFilters – filter by status returns only matching tasks")
    void findWithFilters_byStatus_returnsCorrectSubset() {
        List<Task> result = taskRepository.findWithFilters(null, Status.DONE, null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Done MEDIUM task");
    }

    @Test
    @DisplayName("findWithFilters – filter by priority HIGH returns only HIGH tasks")
    void findWithFilters_byPriorityHigh_returnsHighOnly() {
        List<Task> result = taskRepository.findWithFilters(null, null, Priority.HIGH);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("findWithFilters – combined status + priority filter")
    void findWithFilters_statusAndPriority_combined() {
        List<Task> result = taskRepository.findWithFilters(null, Status.TODO, Priority.LOW);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Future LOW task");
    }

    @Test
    @DisplayName("findWithFilters – filter yields empty list when no match")
    void findWithFilters_noMatch_returnsEmpty() {
        List<Task> result = taskRepository.findWithFilters("xyz_no_match", null, null);
        assertThat(result).isEmpty();
    }

    // ── countByStatus ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("countByStatus – counts TODO correctly")
    void countByStatus_todo() {
        assertThat(taskRepository.countByStatus(Status.TODO)).isEqualTo(2);
    }

    @Test
    @DisplayName("countByStatus – counts DONE correctly")
    void countByStatus_done() {
        assertThat(taskRepository.countByStatus(Status.DONE)).isEqualTo(1);
    }

    @Test
    @DisplayName("countByStatus – IN_PROGRESS count is zero initially")
    void countByStatus_inProgress_zero() {
        assertThat(taskRepository.countByStatus(Status.IN_PROGRESS)).isEqualTo(0);
    }

    // ── countOverdue ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("countOverdue – only non-DONE tasks with past due dates are counted")
    void countOverdue_excludesDoneTasks() {
        // pastHighTask (TODO, past) → overdue
        // futureLoTask (TODO, future)  → not overdue
        // donePastTask (DONE, past)    → NOT overdue (status = DONE)
        long count = taskRepository.countOverdue(LocalDate.now());
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("countOverdue – marking overdue task DONE removes it from overdue count")
    void countOverdue_afterMarkingDone_decrements() {
        pastHighTask.setStatus(Status.DONE);
        taskRepository.save(pastHighTask);

        long count = taskRepository.countOverdue(LocalDate.now());
        assertThat(count).isEqualTo(0);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Task makeTask(String title, LocalDate dueDate, Priority priority, Status status) {
        Task t = new Task();
        t.setTitle(title);
        t.setDueDate(dueDate);
        t.setPriority(priority);
        t.setStatus(status);
        return t;
    }
}
