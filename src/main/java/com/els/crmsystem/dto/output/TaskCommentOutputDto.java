package com.els.crmsystem.dto.output;
import java.time.LocalDateTime;

public record TaskCommentOutputDto(
        Long id,
        String text,
        String authorName,
        LocalDateTime createdAt
) {}
