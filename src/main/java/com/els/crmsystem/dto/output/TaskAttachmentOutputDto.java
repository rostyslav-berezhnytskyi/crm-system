package com.els.crmsystem.dto.output;

import java.time.LocalDateTime;

public record TaskAttachmentOutputDto(
        Long id,
        String fileName,
        String fileUrl,
        LocalDateTime uploadedAt
) {}
