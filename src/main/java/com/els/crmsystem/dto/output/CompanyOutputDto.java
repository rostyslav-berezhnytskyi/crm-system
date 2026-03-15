package com.els.crmsystem.dto.output;

import java.util.List;

public record CompanyOutputDto(
        Long id,
        String name,
        String website,
        String mainPhone,
        String email,
        String notes,
        boolean active,
        List<ContactOutputDto> contacts
) {}
