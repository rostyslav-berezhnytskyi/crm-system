package com.els.crmsystem.controller.api;

import com.els.crmsystem.dto.input.TaskInputDto;
import com.els.crmsystem.mapper.EntityMapper;
import com.els.crmsystem.service.TaskAttachmentService;
import com.els.crmsystem.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskRestController {

    private final TaskService taskService;
    private final EntityMapper mapper;
    private final TaskAttachmentService taskAttachmentService; // ADD THIS

    // 1. Create a new task via AJAX
    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody TaskInputDto dto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        taskService.createTask(dto, username);
        return ResponseEntity.ok().body("Task created successfully");
    }

    // 2. Delete a task via AJAX
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.ok().body("Task deleted successfully");
    }

    // 3. The Magic Drag-and-Drop Endpoint
    // Expects: /api/tasks/5/move?newGroupId=2
    // Body: [5, 12, 8] (The new order of Task IDs in that column)
    @PutMapping("/{taskId}/move")
    public ResponseEntity<?> moveTask(
            @PathVariable Long taskId,
            @RequestParam Long newGroupId,
            @RequestBody List<Long> orderedTaskIds) {

        taskService.updateTaskBoardPosition(taskId, newGroupId, orderedTaskIds);
        return ResponseEntity.ok().build();
    }

    // Fetch full task details for the Modal
    @GetMapping("/{id}")
    public ResponseEntity<com.els.crmsystem.dto.output.TaskOutputDto> getTask(@PathVariable Long id) {
        com.els.crmsystem.entity.Task task = taskService.getTaskById(id); // Ensure getTaskById exists in TaskService!
        return ResponseEntity.ok(mapper.toOutputDto(task));
    }

    // Toggle completion status
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleTaskCompletion(@PathVariable Long id) {
        taskService.toggleTaskCompletion(id);
        return ResponseEntity.ok().build();
    }

    // 4. Upload Attachment
    @PostMapping("/{taskId}/attachments")
    public ResponseEntity<?> uploadAttachment(@PathVariable Long taskId, @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        taskAttachmentService.uploadAttachment(taskId, file);
        return ResponseEntity.ok().body("File uploaded successfully");
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @RequestBody TaskInputDto dto) {
        // Ми просто перевикористаємо логіку сервісу.
        // Найпростіший спосіб - додати метод update у TaskService
        taskService.updateTask(id, dto);
        return ResponseEntity.ok().build();
    }
}
