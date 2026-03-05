package com.els.crmsystem.dto.output;

import java.time.LocalDateTime;

public record ProjectMediaOutputDto(
        Long id,
        String fileUrl,
        String fileType,
        String description,
        LocalDateTime uploadedAt
) {}
