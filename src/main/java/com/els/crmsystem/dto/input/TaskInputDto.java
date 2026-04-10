package com.els.crmsystem.dto.input;

import com.els.crmsystem.enums.TaskPriority;
import java.time.LocalDateTime;

public record TaskInputDto(
        String title,
        String description,
        TaskPriority priority,
        LocalDateTime dueDate,
        Long groupId,       // REQUIRED: Which column it belongs to
        Long assigneeId,    // OPTIONAL: Who is doing it
        Long projectId,     // OPTIONAL Context
        Long companyId,     // OPTIONAL Context
        Long contactId,     // OPTIONAL Context
        Long parentTaskId   // OPTIONAL: If this is a subtask
) {}
