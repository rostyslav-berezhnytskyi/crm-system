package com.els.crmsystem.repository;

import com.els.crmsystem.entity.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // 1. For the Trello Board: Get all tasks in a specific column, ordered top-to-bottom
    List<Task> findByGroupIdOrderByDisplayOrderAsc(Long groupId);

    // 2. For Profile Pages: Get all tasks related to a specific entity
    List<Task> findByLinkedCompanyIdOrderByCreatedAtDesc(Long companyId);
    List<Task> findByLinkedContactIdOrderByCreatedAtDesc(Long contactId);
    List<Task> findByLinkedProjectIdOrderByCreatedAtDesc(Long projectId);

    // 3. For finding where to put a new task (gets the bottom position of a column)
    @Query("SELECT COALESCE(MAX(t.displayOrder), 0) FROM Task t WHERE t.group.id = :groupId")
    int findMaxDisplayOrderByGroupId(@Param("groupId") Long groupId);

    // Fetch all tasks for a specific contact, optionally ordered by due date or status
    List<Task> findByLinkedContactId(Long contactId);

    List<Task> findByLinkedCompanyId(Long companyId);

    List<Task> findByLinkedProjectId(Long projectId);

    Page<Task> findByCompletedTrue(Pageable pageable);
}
