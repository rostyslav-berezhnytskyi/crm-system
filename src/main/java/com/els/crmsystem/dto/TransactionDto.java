package com.els.crmsystem.dto;

import com.els.crmsystem.enums.PaymentMethod;
import com.els.crmsystem.enums.TransactionCategory;
import com.els.crmsystem.enums.TransactionType;
import jakarta.validation.constraints.NotNull; // Requires 'spring-boot-starter-validation'
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransactionDto(
        @NotNull(message = "ProjectService ID is required")
        Long projectId,

        @NotNull(message = "Type (INCOME/EXPENSE) is required")
        TransactionType type,

        @NotNull
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull
        TransactionCategory category,

        @NotNull
        PaymentMethod paymentMethod,

        String description
) {}
