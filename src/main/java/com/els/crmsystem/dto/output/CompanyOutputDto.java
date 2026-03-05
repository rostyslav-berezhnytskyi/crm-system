package com.els.crmsystem.dto.output;

public record CompanyOutputDto(
        Long id,
        String name,
        String website,
        String mainPhone,
        String email,
        String notes,
        boolean active
) {}
