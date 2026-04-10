package com.els.crmsystem.controller.api;

import com.els.crmsystem.dto.input.TaskInputDto;
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
}
