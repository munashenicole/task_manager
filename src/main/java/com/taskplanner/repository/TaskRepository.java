package com.taskplanner.repository;

import com.taskplanner.enums.Priority;
import com.taskplanner.enums.Status;
import com.taskplanner.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    /**
     * Filter by optional status and/or priority, with optional title search.
     * All parameters are nullable – null means "no filter on that field".
     */
    @Query("""
        SELECT t FROM Task t
        WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', COALESCE(:title, ''), '%'))
          AND (:status   IS NULL OR t.status   = :status)
          AND (:priority IS NULL OR t.priority = :priority)
        ORDER BY t.createdAt DESC
        """)
    List<Task> findWithFilters(
            @Param("title")    String title,
            @Param("status")   Status status,
            @Param("priority") Priority priority
    );

    /** Count tasks by status (used for the dashboard summary). */
    long countByStatus(Status status);

    /**
     * Count tasks that are overdue: due date strictly before today and not Done.
     */
    @Query("SELECT COUNT(t) FROM Task t WHERE t.dueDate < :today AND t.status <> 'DONE'")
    long countOverdue(@Param("today") LocalDate today);
}
