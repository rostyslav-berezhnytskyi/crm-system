package com.els.crmsystem.dto.input;

import com.els.crmsystem.enums.CompanyRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompanyInputDto(
        @NotBlank(message = "Company name is required")
        String name,
        @NotNull(message = "Role is required")
        CompanyRole role,
        String website,
        String mainPhone,
        @Email(message = "Invalid email format")
        String email,
        String notes
) {}
