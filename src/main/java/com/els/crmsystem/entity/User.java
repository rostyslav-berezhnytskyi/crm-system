package com.els.crmsystem.entity;

import com.els.crmsystem.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Username is required")
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /**
     * If false, user cannot log in.
     * Useful for banning ex-employees without deleting their data history.
     */
    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Stores the file path to the user's uploaded profile picture.
     */
    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "telegram_id", length = 50)
    private String telegramId;

    @Column(name = "locked")
    private boolean locked = false;

    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts = 0;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}