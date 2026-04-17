package com.els.crmsystem.dto.output;

import com.els.crmsystem.enums.TaskPriority;
import java.time.LocalDateTime;
import java.util.List;

public record TaskOutputDto(
        Long id,
        String title,
        String description,
        TaskPriority priority,
        int displayOrder,
        LocalDateTime createdAt,
        LocalDateTime dueDate,
        boolean completed,

        // Flattened relationships (just names/IDs, not full objects)
        Long groupId,
        String groupName,
        String creatorName,
        Long assigneeId,
        String assigneeName,

        // Context badges for the Trello card
        Long projectId,
        String projectName,
        Long companyId,
        String companyName,
        Long contactId,
        String contactName,

        // Nested Data for the Details Modal!
        List<TaskAttachmentOutputDto>attachments,
        List<TaskOutputDto> subtasks
) {}
