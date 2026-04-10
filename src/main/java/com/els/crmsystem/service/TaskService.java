package com.els.crmsystem.service;

import com.els.crmsystem.dto.input.TaskInputDto;
import com.els.crmsystem.dto.output.TaskOutputDto;
import com.els.crmsystem.entity.*;
import com.els.crmsystem.mapper.EntityMapper;
import com.els.crmsystem.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final CompanyRepository companyRepository;
    private final ContactRepository contactRepository;
    private final EntityMapper mapper; // You will need to add toOutputDto(Task task) to this!

    @Transactional
    public void createTask(TaskInputDto dto, String currentUsername) {
        log.info("User {} is creating a new task: {}", currentUsername, dto.title());

        Task task = new Task();
        task.setTitle(dto.title());
        task.setDescription(dto.description());
        task.setPriority(dto.priority());
        task.setDueDate(dto.dueDate());

        // 1. REQUIRED: Set the Creator
        User creator = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Creator not found"));
        task.setCreator(creator);

        // 2. REQUIRED: Set the Group (Column) and calculate its position
        TaskGroup group = groupRepository.findById(dto.groupId())
                .orElseThrow(() -> new IllegalArgumentException("Task Group not found"));
        task.setGroup(group);

        // Put the new task at the bottom of the column
        int nextOrder = taskRepository.findMaxDisplayOrderByGroupId(group.getId()) + 1;
        task.setDisplayOrder(nextOrder);

        // 3. OPTIONAL: Link Assignee
        if (dto.assigneeId() != null) {
            userRepository.findById(dto.assigneeId()).ifPresent(task::setAssignee);
        }

        // 4. OPTIONAL: Link CRM Contexts (Project, Company, Contact)
        if (dto.projectId() != null) {
            projectRepository.findById(dto.projectId()).ifPresent(task::setLinkedProject);
        }
        if (dto.companyId() != null) {
            companyRepository.findById(dto.companyId()).ifPresent(task::setLinkedCompany);
        }
        if (dto.contactId() != null) {
            contactRepository.findById(dto.contactId()).ifPresent(task::setLinkedContact);
        }

        // 5. OPTIONAL: Link Parent Task (if it's a subtask)
        if (dto.parentTaskId() != null) {
            taskRepository.findById(dto.parentTaskId()).ifPresent(task::setParentTask);
        }

        taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        // Because of CascadeType.ALL on attachments and subtasks,
        // deleting this task will automatically wipe out everything attached to it!
        // NOTE: Make sure you call TaskAttachmentService to delete physical files first if needed.
        taskRepository.deleteById(taskId);
    }

    // Example of how you will fetch data for the Trello Board
    @Transactional(readOnly = true)
    public List<TaskOutputDto> getTasksForGroup(Long groupId) {
        return taskRepository.findByGroupIdOrderByDisplayOrderAsc(groupId).stream()
                .map(mapper::toOutputDto) // You need to create this mapping logic
                .collect(Collectors.toList());
    }

    /**
     * Updates a task's group and reorders the entire column.
     * This is the magic method for Trello-style drag-and-drop.
     */
    @Transactional
    public void updateTaskBoardPosition(Long taskId, Long newGroupId, List<Long> orderedTaskIdsInNewGroup) {
        // 1. Find the task and the new group
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        TaskGroup newGroup = groupRepository.findById(newGroupId)
                .orElseThrow(() -> new IllegalArgumentException("Task Group not found"));

        // 2. Move the task to the new column
        task.setGroup(newGroup);
        taskRepository.save(task);

        // 3. Update the displayOrder for EVERY task in that column
        // based on the array sent by the frontend JavaScript
        for (int i = 0; i < orderedTaskIdsInNewGroup.size(); i++) {
            Long id = orderedTaskIdsInNewGroup.get(i);
            Task t = taskRepository.findById(id).orElse(null);
            if (t != null) {
                t.setDisplayOrder(i + 1); // 1, 2, 3, 4...
                taskRepository.save(t);
            }
        }
    }
}
