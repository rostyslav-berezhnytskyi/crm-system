package com.els.crmsystem.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public record ProjectDto(
        Long id,

        @NotBlank(message = "Project name cannot be empty")
        String name,

        String description,

        boolean active, // True = Open, False = Closed/Archived

        LocalDateTime createdDate
) {}
