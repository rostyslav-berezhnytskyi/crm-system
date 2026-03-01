package com.els.crmsystem.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String website;

    @Column(name = "main_phone", length = 50)
    private String mainPhone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false)
    private boolean active = true;

    // A company can have many contacts (managers, support staff, etc.)
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL)
    private List<Contact> contacts = new ArrayList<>();

    // A company can have many official documents (PDFs, photos, etc.)
    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompanyDocument> documents = new ArrayList<>();
}