package com.els.crmsystem.entity;

import com.els.crmsystem.enums.TaskPriority;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description; // Unlimited text for complex details

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(name = "display_order", nullable = false)
    private int displayOrder; // Magic field: saves where you dragged it in the column!

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "is_completed", nullable = false)
    private boolean completed = false;

    // --- USERS ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false, updatable = false)
    private User creator; // The person who clicked "Create"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee; // The person who actually has to do the work

    // --- RELATIONSHIPS ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private TaskGroup group; // Which column does it live in?

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project linkedProject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company linkedCompany;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact linkedContact;

    // --- SUBTASKS (Self-Referencing) ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private Task parentTask; // If this is a subtask, who is the parent?

    @OneToMany(mappedBy = "parentTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> subtasks = new ArrayList<>();

    // --- ATTACHMENTS (Photos/Docs) ---
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskAttachment> attachments = new ArrayList<>();

    // --- COMMENTS (Chat) ---
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC") // Oldest messages at the top, newest at the bottom
    private List<TaskComment> comments = new ArrayList<>();

    // Auto-fill the creation date
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
