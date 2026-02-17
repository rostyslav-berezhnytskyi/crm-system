package com.els.crmsystem.dto.input;

import com.els.crmsystem.enums.PaymentMethod;
import com.els.crmsystem.enums.TransactionCategory;
import com.els.crmsystem.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * INPUT DTO: Handles data coming FROM the user (via HTML Form).
 * Contains validation rules and handles File Uploads.
 */
public record TransactionInputDto(

        @NotNull(message = "You must select a Project")
        Long projectId,

        @NotNull(message = "Transaction Type is required")
        TransactionType type,

        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotNull(message = "Category is required")
        TransactionCategory category,

        @NotNull(message = "Payment Method is required")
        PaymentMethod paymentMethod,

        @Size(max = 1000, message = "Description is too long (max 1000 chars)")
        String description,

        /**
         * Optional: If null, the system sets it to LocalDateTime.now()
         * Used for "Backdating" an expense.
         */
        LocalDateTime date,

        // --- FILE UPLOADS (Input Only) ---

        /**
         * The raw file for the receipt/invoice (PDF, JPG, PNG).
         * Spring converts the HTML file input into this object.
         */
        MultipartFile receiptFile,

        /**
         * The raw file for the item photo (Evidence of purchase).
         */
        MultipartFile itemImageFile
) {}