package com.taskplanner.controller;

import com.taskplanner.enums.Priority;
import com.taskplanner.enums.Status;
import com.taskplanner.model.Task;
import com.taskplanner.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

/**
 * MVC controller – all routes return Thymeleaf view names.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    // ── List / Dashboard ───────────────────────────────────────────────────────

    @GetMapping
    public String list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Priority priority,
            Model model) {

        model.addAttribute("tasks",      taskService.findAll(search, status, priority));
        model.addAttribute("stats",      taskService.getStats());
        model.addAttribute("statuses",   Status.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("search",     search);
        model.addAttribute("selStatus",  status);
        model.addAttribute("selPriority",priority);
        return "index";
    }

    // ── Redirect root to /tasks ────────────────────────────────────────────────

    @GetMapping("/")
    public String root() {
        return "redirect:/tasks";
    }

    // ── Create ─────────────────────────────────────────────────────────────────

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("task",       new Task());
        model.addAttribute("statuses",   Status.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("formAction", "/tasks");
        model.addAttribute("pageTitle",  "New Task");
        return "form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("task") Task task,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("statuses",   Status.values());
            model.addAttribute("priorities", Priority.values());
            model.addAttribute("formAction", "/tasks");
            model.addAttribute("pageTitle",  "New Task");
            return "form";
        }

        try {
            taskService.create(task);
            redirectAttributes.addFlashAttribute("successMsg", "Task created successfully!");
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg",   e.getMessage());
            model.addAttribute("statuses",   Status.values());
            model.addAttribute("priorities", Priority.values());
            model.addAttribute("formAction", "/tasks");
            model.addAttribute("pageTitle",  "New Task");
            return "form";
        }
        return "redirect:/tasks";
    }

    // ── Edit ───────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable UUID id, Model model) {
        Task task = taskService.findById(id);
        model.addAttribute("task",       task);
        model.addAttribute("statuses",   Status.values());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("formAction", "/tasks/" + id);
        model.addAttribute("pageTitle",  "Edit Task");
        return "form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable UUID id,
            @Valid @ModelAttribute("task") Task task,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            model.addAttribute("statuses",   Status.values());
            model.addAttribute("priorities", Priority.values());
            model.addAttribute("formAction", "/tasks/" + id);
            model.addAttribute("pageTitle",  "Edit Task");
            return "form";
        }

        try {
            taskService.update(id, task);
            redirectAttributes.addFlashAttribute("successMsg", "Task updated successfully!");
        } catch (IllegalArgumentException e) {
            model.addAttribute("errorMsg",   e.getMessage());
            model.addAttribute("statuses",   Status.values());
            model.addAttribute("priorities", Priority.values());
            model.addAttribute("formAction", "/tasks/" + id);
            model.addAttribute("pageTitle",  "Edit Task");
            return "form";
        }
        return "redirect:/tasks";
    }

    // ── Delete ─────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            taskService.delete(id);
            redirectAttributes.addFlashAttribute("successMsg", "Task deleted.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", "Could not delete task: " + e.getMessage());
        }
        return "redirect:/tasks";
    }

    // ── Quick status change ────────────────────────────────────────────────────

    @PostMapping("/{id}/status")
    public String changeStatus(
            @PathVariable UUID id,
            @RequestParam Status status,
            RedirectAttributes redirectAttributes) {

        try {
            Task task = taskService.findById(id);
            task.setStatus(status);
            taskService.update(id, task);
            redirectAttributes.addFlashAttribute("successMsg",
                    "Status updated to " + status.name().replace("_", " ") + ".");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/tasks";
    }
}
