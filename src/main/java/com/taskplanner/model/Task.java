package com.taskplanner.model;

import com.taskplanner.enums.Priority;
import com.taskplanner.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a Task in the planner.
 * <p>
 * Timezone note: All {@code createdAt} and {@code updatedAt} timestamps are
 * persisted as {@link LocalDateTime} and interpreted in the JVM's configured
 * timezone (<strong>Africa/Harare, CAT UTC+2, no DST</strong>) as set in
 * {@code application.properties}.  Overdue detection compares {@code dueDate}
 * against {@code LocalDate.now()} using that same JVM timezone.
 * </p>
 */
@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
public class Task {

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @NotBlank(message = "Title must not be empty")
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Due date is required")
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @NotNull(message = "Priority is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private Priority priority;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = Status.TODO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * A task is overdue when its due date is strictly before today AND its
     * status is not {@link Status#DONE}.
     */
    @Transient
    public boolean isOverdue() {
        return dueDate != null
                && dueDate.isBefore(LocalDate.now())
                && status != Status.DONE;
    }
}
