package com.els.crmsystem.dto.input;

import com.els.crmsystem.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserEditDto(
        @NotBlank(message = "Username is required")
        String username,

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email,

        String phoneNumber,
        String telegramId,
        Role role,
        boolean enabled,
        Boolean locked,
        String newPassword // Optional!
) {}
