package com.els.crmsystem.dto.output;

import com.els.crmsystem.enums.ContactRole;
import java.time.LocalDateTime;

public record ContactOutputDto(
        Long id,
        Long companyId,
        String companyName, // Extremely useful for the HTML table!
        String name,
        ContactRole role,
        String phone,
        String email,
        String notes,
        boolean active,
        LocalDateTime lastContactDate
) {}
