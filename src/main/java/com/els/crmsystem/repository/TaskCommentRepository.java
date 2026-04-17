package com.els.crmsystem.repository;

import com.els.crmsystem.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskCommentRepository extends JpaRepository<TaskComment, Long> {
}
