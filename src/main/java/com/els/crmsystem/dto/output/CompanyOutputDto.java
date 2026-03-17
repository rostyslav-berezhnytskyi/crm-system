package com.els.crmsystem.dto.output;

import com.els.crmsystem.enums.CompanyRole;

import java.util.List;

public record CompanyOutputDto(
        Long id,
        String name,
        CompanyRole role,
        String website,
        String mainPhone,
        String email,
        String notes,
        boolean active,
        List<ContactOutputDto> contacts
) {}
