package com.els.crmsystem.dto.output;

import com.els.crmsystem.enums.PaymentMethod;
import com.els.crmsystem.enums.TransactionCategory;
import com.els.crmsystem.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * OUTPUT DTO: Used to display transaction rows in the UI Table.
 * Contains formatted data and File URLs (String paths), not raw files.
 */
public record TransactionOutputDto(
        Long id,                // Required for "Edit" and "Delete" links

        String projectName,     // Resolved name (e.g., "Kyiv Office") instead of just ID

        TransactionType type,
        BigDecimal amount,
        TransactionCategory category,
        PaymentMethod paymentMethod,
        String description,

        LocalDateTime date,     // The actual business date of the transaction

        // --- FILE LINKS (Output Only) ---

        /**
         * The relative URL to download/view the receipt.
         * Example: "f47ac10b-receipt.pdf"
         */
        String receiptUrl,

        /**
         * The relative URL to view the item photo.
         * Example: "a1b2c3d4-cable.jpg"
         */
        String itemImageUrl
) {}