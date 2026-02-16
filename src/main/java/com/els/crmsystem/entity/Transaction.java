package com.els.crmsystem.entity;

import com.els.crmsystem.enums.PaymentMethod;
import com.els.crmsystem.enums.TransactionCategory;
import com.els.crmsystem.enums.TransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a financial record in the system.
 * This can be an income (earning) or an expense (spending) linked to a specific Project and User.
 */
@Entity
@Getter
@Setter
@ToString(exclude = {"user", "project"}) // Prevent infinite loops in logs
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The financial type: INCOME (Money In) or EXPENSE (Money Out).
     */
    @NotNull(message = "Transaction type is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    /**
     * A human-readable explanation of the transaction.
     * Example: "Bought 50m of copper cable for generator connection"
     */
    @Column(nullable = false, length = 1000) // Increased length for detailed notes
    private String description;

    /**
     * The monetary value.
     * Precision 19, Scale 2 allows values up to 99,999,999,999,999,999.99
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * How the money was moved (CASH, CARD, IBAN).
     */
    @NotNull(message = "Payment method is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    /**
     * Business category for reporting (e.g., MATERIALS, SALARY, OFFICE).
     */
    @NotNull(message = "Category is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransactionCategory category;

    // --- FILE ATTACHMENTS ---

    /**
     * Relative path to the uploaded receipt/invoice image or PDF.
     * Stored in the 'uploads' directory.
     */
    @Column(name = "receipt_url")
    private String receiptUrl;

    /**
     * Relative path to the photo of the physical item/result.
     * Useful for verifying that materials were actually purchased/installed.
     */
    @Column(name = "item_image_url")
    private String itemImageUrl;

    /**
     * The exact time the transaction was recorded in the system.
     */
    @Column(nullable = false)
    private LocalDateTime date;

    // --- RELATIONSHIPS ---

    /**
     * The user who registered this transaction.
     * FetchType.LAZY ensures we don't load the entire User object unless needed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * The project this transaction belongs to.
     * Expenses are deducted from this project's budget.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // --- LIFECYCLE HOOKS ---

    /**
     * Automatically sets the date before the entity is first saved to the database.
     */
    @PrePersist
    protected void onCreate() {
        // Only set current time if the user didn't provide a specific date
        if (this.date == null) {
            this.date = LocalDateTime.now();
        }
    }
}