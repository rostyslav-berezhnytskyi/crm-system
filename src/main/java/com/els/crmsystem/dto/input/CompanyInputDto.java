package com.els.crmsystem.dto.input;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CompanyInputDto(
        @NotBlank(message = "Company name is required")
        String name,
        String website,
        String mainPhone,
        @Email(message = "Invalid email format")
        String email,
        String notes
) {}
