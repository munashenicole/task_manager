package com.taskplanner.controller;

import com.taskplanner.enums.Priority;
import com.taskplanner.enums.Status;
import com.taskplanner.model.Task;
import com.taskplanner.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MVC slice tests for {@link TaskController}.
 * Only the web layer is loaded; the service is mocked.
 */
@WebMvcTest(TaskController.class)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    private Task sampleTask;
    private UUID taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID();
        sampleTask = new Task();
        sampleTask.setTitle("Sample Task");
        sampleTask.setDueDate(LocalDate.now().plusDays(5));
        sampleTask.setPriority(Priority.MEDIUM);
        sampleTask.setStatus(Status.TODO);

        try {
            var id = Task.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(sampleTask, taskId);
            var ca = Task.class.getDeclaredField("createdAt");
            ca.setAccessible(true);
            ca.set(sampleTask, LocalDateTime.now());
            var ua = Task.class.getDeclaredField("updatedAt");
            ua.setAccessible(true);
            ua.set(sampleTask, LocalDateTime.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        when(taskService.getStats()).thenReturn(
                Map.of("todo", 1L, "inProgress", 0L, "done", 0L, "overdue", 0L));
    }

    // ── GET /tasks ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /tasks – returns HTTP 200 and renders index view")
    void list_returnsOkAndIndexView() throws Exception {
        when(taskService.findAll(null, null, null)).thenReturn(List.of(sampleTask));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("tasks", "stats", "statuses", "priorities"));
    }

    @Test
    @DisplayName("GET /tasks?priority=HIGH – filter param is forwarded to service")
    void list_withPriorityFilter_passesParamToService() throws Exception {
        when(taskService.findAll(null, null, Priority.HIGH)).thenReturn(List.of());

        mockMvc.perform(get("/tasks").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selPriority", Priority.HIGH));

        verify(taskService).findAll(null, null, Priority.HIGH);
    }

    // ── GET /tasks/new ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /tasks/new – returns form view with empty task")
    void showCreateForm_returnsFormView() throws Exception {
        mockMvc.perform(get("/tasks/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("form"))
                .andExpect(model().attributeExists("task", "statuses", "priorities"));
    }

    // ── POST /tasks ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /tasks – valid data redirects to /tasks")
    void create_validData_redirectsToDashboard() throws Exception {
        when(taskService.create(any(Task.class))).thenReturn(sampleTask);

        mockMvc.perform(post("/tasks")
                        .param("title", "Test Task")
                        .param("dueDate", LocalDate.now().plusDays(1).toString())
                        .param("priority", "HIGH")
                        .param("status", "TODO"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"));
    }

    @Test
    @DisplayName("POST /tasks – blank title returns form with validation error")
    void create_blankTitle_returnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/tasks")
                        .param("title", "")
                        .param("dueDate", LocalDate.now().plusDays(1).toString())
                        .param("priority", "HIGH")
                        .param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(view().name("form"))
                .andExpect(model().hasErrors())
                .andExpect(model().attributeHasFieldErrors("task", "title"));
    }

    @Test
    @DisplayName("POST /tasks – missing due date returns form with validation error")
    void create_missingDueDate_returnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/tasks")
                        .param("title", "Valid Title")
                        .param("priority", "HIGH")
                        .param("status", "TODO"))
                .andExpect(status().isOk())
                .andExpect(view().name("form"))
                .andExpect(model().hasErrors());
    }

    // ── GET /tasks/{id}/edit ──────────────────────────────────────────────────

    @Test
    @DisplayName("GET /tasks/{id}/edit – known id returns form view populated with task")
    void showEditForm_knownId_returnsFormView() throws Exception {
        when(taskService.findById(taskId)).thenReturn(sampleTask);

        mockMvc.perform(get("/tasks/{id}/edit", taskId))
                .andExpect(status().isOk())
                .andExpect(view().name("form"))
                .andExpect(model().attribute("task", sampleTask));
    }

    @Test
    @DisplayName("GET /tasks/{id}/edit – unknown id propagates NoSuchElementException")
    void showEditForm_unknownId_throwsNoSuchElement() {
        UUID unknown = UUID.randomUUID();
        when(taskService.findById(unknown)).thenThrow(new NoSuchElementException("Task not found: " + unknown));

        // Without a global @ControllerAdvice, MockMvc re-throws the exception
        // wrapped in a NestedServletException — assert the root cause
        assertThatThrownBy(() ->
                mockMvc.perform(get("/tasks/{id}/edit", unknown)))
                .getRootCause()
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Task not found");
    }

    // ── POST /tasks/{id}/status ───────────────────────────────────────────────

    @Test
    @DisplayName("POST /tasks/{id}/status – status update redirects to /tasks")
    void changeStatus_validStatus_redirectsToDashboard() throws Exception {
        when(taskService.findById(taskId)).thenReturn(sampleTask);
        when(taskService.update(eq(taskId), any(Task.class))).thenReturn(sampleTask);

        mockMvc.perform(post("/tasks/{id}/status", taskId)
                        .param("status", "DONE"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"));
    }

    // ── POST /tasks/{id}/delete ───────────────────────────────────────────────

    @Test
    @DisplayName("POST /tasks/{id}/delete – successful delete redirects with flash message")
    void delete_existingTask_redirectsWithSuccessMessage() throws Exception {
        doNothing().when(taskService).delete(taskId);

        mockMvc.perform(post("/tasks/{id}/delete", taskId))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tasks"))
                .andExpect(flash().attributeExists("successMsg"));
    }
}
