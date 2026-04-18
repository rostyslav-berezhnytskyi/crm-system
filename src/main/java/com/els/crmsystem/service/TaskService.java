package com.els.crmsystem.service;

import com.els.crmsystem.dto.input.TaskInputDto;
import com.els.crmsystem.dto.output.TaskOutputDto;
import com.els.crmsystem.entity.*;
import com.els.crmsystem.mapper.EntityMapper;
import com.els.crmsystem.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    private final EntityMapper mapper;
    private final AuditNotificationService notificationService;
    private final TaskCommentRepository commentRepository;

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

        // --- TRIGGER UNIVERSAL NOTIFICATION ---
        sendTaskTelegramNotification(task, currentUsername, "🔔 *Нова задача!*", null);
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

    public Task getTaskById(Long id) {
        return taskRepository.findById(id).orElseThrow();
    }

    @Transactional
    public void toggleTaskCompletion(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        // Flip the boolean (if true -> false, if false -> true)
        task.setCompleted(!task.isCompleted());

        // --- RECORD THE EXACT TIME IT WAS COMPLETED ---
        if (task.isCompleted()) {
            task.setCompletedAt(java.time.LocalDateTime.now());
        } else {
            task.setCompletedAt(null); // Clear it if they un-check "Done"
        }

        taskRepository.save(task);
    }

    @Transactional
    public void updateTask(Long id, TaskInputDto dto) {
        Task task = taskRepository.findById(id).orElseThrow();
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

        // 1. CAPTURE THE OLD STATE
        Long oldAssigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;
        java.time.LocalDateTime oldDueDate = task.getDueDate();
        com.els.crmsystem.enums.TaskPriority oldPriority = task.getPriority();
        Long oldGroupId = task.getGroup() != null ? task.getGroup().getId() : null;

        // 2. APPLY THE UPDATES (Your existing logic)
        task.setTitle(dto.title());
        task.setDescription(dto.description());
        task.setPriority(dto.priority());
        task.setDueDate(dto.dueDate());

        if (dto.groupId() != null) {
            TaskGroup group = groupRepository.findById(dto.groupId()).orElseThrow();
            task.setGroup(group);
        }

        task.setAssignee(dto.assigneeId() != null ? userRepository.findById(dto.assigneeId()).orElse(null) : null);
        task.setLinkedProject(dto.projectId() != null ? projectRepository.findById(dto.projectId()).orElse(null) : null);
        task.setLinkedCompany(dto.companyId() != null ? companyRepository.findById(dto.companyId()).orElse(null) : null);
        task.setLinkedContact(dto.contactId() != null ? contactRepository.findById(dto.contactId()).orElse(null) : null);

        taskRepository.save(task);

        // 3. SMART NOTIFICATION LOGIC
        if (task.getAssignee() != null && task.getAssignee().getTelegramId() != null) {
            StringBuilder changes = new StringBuilder();

            // Check if it was just assigned to a NEW person
            boolean isNewlyAssigned = oldAssigneeId == null || !oldAssigneeId.equals(task.getAssignee().getId());

            if (isNewlyAssigned) {
                changes.append("👉 *Вам передано цю задачу!*\n");
            } else {
                // Only check these if it's the SAME person, so we tell them what changed
                if (oldPriority != task.getPriority()) {
                    changes.append(String.format("🚩 Пріоритет змінено: *%s*\n", task.getPriority().getUkrainianName()));
                }

                // Compare groups safely
                Long newGroupId = task.getGroup() != null ? task.getGroup().getId() : null;
                if (oldGroupId != null && !oldGroupId.equals(newGroupId)) {
                    changes.append(String.format("📋 Переміщено в колонку: *%s*\n", task.getGroup().getName()));
                }

                // Compare dates safely
                String oldDateStr = oldDueDate != null ? java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(oldDueDate) : "Немає";
                String newDateStr = task.getDueDate() != null ? java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(task.getDueDate()) : "Немає";
                if (!oldDateStr.equals(newDateStr)) {
                    changes.append(String.format("⏳ Новий дедлайн: *%s*\n", newDateStr));
                }
            }

            // 4. SEND NOTIFICATION IF SOMETHING CRITICAL CHANGED
            if (changes.length() > 0) {
                sendTaskTelegramNotification(task, currentUsername, "⚠️ *Оновлення задачі!*", changes.toString());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<TaskOutputDto> getTasksForContact(Long contactId) {
        return taskRepository.findByLinkedContactId(contactId).stream()
                .map(mapper::toOutputDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskOutputDto> getTasksForCompany(Long companyId) {
        return taskRepository.findByLinkedCompanyId(companyId).stream()
                .map(mapper::toOutputDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskOutputDto> getTasksForProject(Long projectId) {
        return taskRepository.findByLinkedProjectId(projectId).stream()
                .map(mapper::toOutputDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<TaskOutputDto> getAllCompletedTasks(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dueDate").descending());
        // You will need to add findByCompletedTrue(Pageable pageable) to TaskRepository!
        return taskRepository.findByCompletedTrue(pageable)
                .map(mapper::toOutputDto);
    }

    // --- UNIVERSAL TELEGRAM NOTIFICATION BUILDER ---
    private void sendTaskTelegramNotification(Task task, String currentUsername, String titlePrefix, String changesText) {

        // 1. Format Context safely
        String projName = task.getLinkedProject() != null ? task.getLinkedProject().getName() : "—";
        String compName = task.getLinkedCompany() != null ? task.getLinkedCompany().getName() : "—";
        String contactName = task.getLinkedContact() != null ? task.getLinkedContact().getName() : "—";
        String groupName = task.getGroup() != null ? task.getGroup().getName() : "—";

        String dueStr = task.getDueDate() != null
                ? java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(task.getDueDate())
                : "Без дедлайну";

        String description = (task.getDescription() != null && !task.getDescription().trim().isEmpty())
                ? task.getDescription()
                : "Немає";

        // 2. Build the Universal Message
        StringBuilder message = new StringBuilder();
        message.append(titlePrefix).append("\n\n");
        message.append("📌 *Назва:* ").append(task.getTitle()).append("\n");

        // If it's an update, show what changed. If it's new, show the description.
        if (changesText != null && !changesText.isEmpty()) {
            message.append("\n🔄 *Що змінилося:*\n").append(changesText).append("\n");
        } else {
            message.append("📝 *Опис:* ").append(description).append("\n\n");
        }

        // Add the full context footprint
        message.append("👤 *Автор/Змінив:* ").append(currentUsername).append("\n");
        message.append("🚩 *Пріоритет:* ").append(task.getPriority().getUkrainianName()).append("\n");
        message.append("📋 *Колонка:* ").append(groupName).append("\n");
        message.append("⏳ *Дедлайн:* ").append(dueStr).append("\n\n");
        message.append("🔗 *Прив'язка:*\n");
        message.append("📁 Проєкт: ").append(projName).append("\n");
        message.append("🏢 Компанія: ").append(compName).append("\n");
        message.append("📞 Контакт: ").append(contactName);

        notifyRelevantUsers(task, currentUsername, message.toString());
    }

    @Transactional
    public void addTaskComment(Long taskId, String text, String currentUsername) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new IllegalArgumentException("Task not found"));
        User author = userRepository.findByUsername(currentUsername).orElseThrow(() -> new RuntimeException("User not found"));

        TaskComment comment = new TaskComment();
        comment.setText(text);
        comment.setTask(task);
        comment.setAuthor(author);

        task.getComments().add(comment);
        taskRepository.save(task);

        // --- SMART NOTIFICATION ---
        String message = String.format(
                "💬 *Новий коментар до задачі!*\n\n" +
                        "📌 *Назва:* %s\n" +
                        "👤 *Від:* %s\n" +
                        "💬 *Повідомлення:* %s",
                task.getTitle(), currentUsername, text
        );
        notifyRelevantUsers(task, currentUsername, message);
    }

    @Transactional
    public void editTaskComment(Long commentId, String newText, String currentUsername) {
        TaskComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        // Security Check: Only the author can edit
        if (!comment.getAuthor().getUsername().equals(currentUsername)) {
            throw new SecurityException("You can only edit your own comments");
        }

        comment.setText(newText);
        commentRepository.save(comment);
    }

    @Transactional
    public void deleteTaskComment(Long commentId, String currentUsername) {
        TaskComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        // Security Check: Only the author can delete
        if (!comment.getAuthor().getUsername().equals(currentUsername)) {
            throw new SecurityException("You can only delete your own comments");
        }

        commentRepository.delete(comment);
    }

    // --- SMART ROUTING: Decides exactly WHO gets the message ---
    private void notifyRelevantUsers(Task task, String currentUsername, String message) {
        java.util.Set<String> notifiedTelegramIds = new java.util.HashSet<>();

        // 1. Check the Assignee
        if (task.getAssignee() != null
                && task.getAssignee().getTelegramId() != null
                && !task.getAssignee().getUsername().equals(currentUsername)) {

            notificationService.sendDirectMessage(task.getAssignee().getTelegramId(), message);
            notifiedTelegramIds.add(task.getAssignee().getTelegramId());
        }

        // 2. Check the Creator
        if (task.getCreator() != null
                && task.getCreator().getTelegramId() != null
                && !task.getCreator().getUsername().equals(currentUsername)) {

            // Prevent double-pinging if the Creator IS the Assignee
            if (!notifiedTelegramIds.contains(task.getCreator().getTelegramId())) {
                notificationService.sendDirectMessage(task.getCreator().getTelegramId(), message);
            }
        }
    }
}
