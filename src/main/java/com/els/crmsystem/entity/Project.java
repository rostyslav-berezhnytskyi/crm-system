package com.els.crmsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Project name cannot be empty")
    @Column(nullable = false, unique = true, length = 100)
    private String name; // e.g., "Kyiv Office Installation"

    @Column(length = 1000)
    private String description; // e.g., "Installation of 30kW Deye + 60kWh Battery"

    /**
     * Status of the project.
     * true = Open/Working
     * false = Closed/Archived (Job done)
     */
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    // --- LIFECYCLE HOOKS ---

    @PrePersist
    protected void onCreate() {
        if (this.createdDate == null) {
            this.createdDate = LocalDateTime.now();
        }
    }
}