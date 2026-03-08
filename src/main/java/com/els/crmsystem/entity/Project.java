package com.els.crmsystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    // Project timeline
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "finish_date")
    private LocalDateTime finishDate;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMedia> media = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Contact client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installer_id")
    private Contact installer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_dealer_id")
    private Company equipmentDealer;

    // Project location
    @Embedded
    private Address address;

    // --- MANY-TO-MANY ADDITIONAL CONTACTS ---
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "project_additional_contacts",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "contact_id")
    )
    private java.util.Set<Contact> additionalContacts = new java.util.HashSet<>();

    // --- LIFECYCLE HOOKS ---

    @PrePersist
    protected void onCreate() {
        if (this.createdDate == null) {
            this.createdDate = LocalDateTime.now();
        }
    }
}