package com.els.crmsystem.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // e.g., "OSBB Installation"

    private String description; // e.g., "Installation of 30kW Deye + 60kWh Battery"

    @Column(nullable = false)
    private boolean active = true; // Default to true. Uncheck this when job is done.

    private LocalDateTime createdDate = LocalDateTime.now(); // Auto-set time
}
