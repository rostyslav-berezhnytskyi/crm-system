package com.els.crmsystem.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectInputDto(
        @NotBlank(message = "Project name cannot be empty")
        @Size(max = 100, message = "Name is too long")
        String name,

        @Size(max = 1000, message = "Description is too long")
        String description,

        // Allowed for Updates (e.g., closing a project)
        // Default to true if null
        Boolean active
) {}