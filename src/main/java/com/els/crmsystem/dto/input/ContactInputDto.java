package com.els.crmsystem.dto.input;

import com.els.crmsystem.enums.ContactRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContactInputDto(
        Long companyId,
        @NotBlank(message = "Contact name is required")
        String name,
        @NotNull(message = "Role is required")
        ContactRole role,
        String phone,
        @Email(message = "Invalid email format")
        String email,
        String notes
) {}
