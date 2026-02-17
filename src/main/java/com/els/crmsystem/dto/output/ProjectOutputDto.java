package com.els.crmsystem.dto.output;

import java.time.LocalDateTime;

public record ProjectOutputDto(
        Long id,           // Server generated
        String name,
        String description,
        boolean active,
        LocalDateTime createdDate // Server generated
) {}