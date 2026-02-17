package com.els.crmsystem.dto.input;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserInputDto(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email,

        String phoneNumber // Optional
) {}