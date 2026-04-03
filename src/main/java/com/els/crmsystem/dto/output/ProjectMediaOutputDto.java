package com.els.crmsystem.dto.output;

import java.time.LocalDateTime;

public record ProjectMediaOutputDto(
        Long id,
        String fileUrl,
        String fileName,  // ADDED FILENAME
        String fileType,
        String folderName,
        String description,
        LocalDateTime uploadedAt
) {}
