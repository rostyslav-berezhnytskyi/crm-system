package com.els.crmsystem.dto.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CalendarEventInputDto(

        @NotBlank(message = "Назва події є обов'язковою")
        @Size(max = 100, message = "Назва не може перевищувати 100 символів")
        String title,

        @Size(max = 500, message = "Опис не може перевищувати 500 символів")
        String description,

        @NotNull(message = "Дата початку є обов'язковою")
        LocalDateTime startDate,

        LocalDateTime endDate,

        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Колір має бути у форматі HEX (#RRGGBB)")
        String color
) {}
