package com.els.crmsystem.repository;

import com.els.crmsystem.entity.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long>, JpaSpecificationExecutor<TaskAttachment> {
    List<TaskAttachment> findByTaskId(Long id);
}
