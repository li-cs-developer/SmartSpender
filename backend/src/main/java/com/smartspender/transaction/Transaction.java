package com.smartspender.transaction;

import com.smartspender.category.Category;
import com.smartspender.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A single income or expense event for a user.
 *
 * Encapsulation:
 *   - Factory constructor enforces all invariants on creation
 *   - No public setters — mutation is intent-revealing (recategorize, moveTo)
 *   - amount is ALWAYS positive; the sign is expressed via `type`
 */
@Entity
@Table(name = "transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Nullable — a transaction can be uncategorized. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TransactionType type;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 500)
    private String notes;

    @Column(name = "txn_date", nullable = false)
    private LocalDate date;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ---- Factory constructor — the ONLY way to create a new Transaction ----

    public Transaction(User user,
                       Category category,
                       BigDecimal amount,
                       TransactionType type,
                       String name,
                       String notes,
                       LocalDate date) {
        this.user = requireNonNull(user, "user");
        this.amount = validateAmount(amount);
        this.type = requireNonNull(type, "type");
        this.name = requireNonBlank(name, "name").trim();
        this.notes = normalizeNotes(notes);
        this.date = requireNonNull(date, "date");
        this.category = category; // ownership verified by the service
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // ---- Intent-revealing mutation ----

    public void recategorize(Category newCategory) {
        this.category = newCategory;
        this.updatedAt = Instant.now();
    }

    public void attachNotes(String newNotes) {
        this.notes = normalizeNotes(newNotes);
        this.updatedAt = Instant.now();
    }

    // ---- Domain validation (private) ----

    private static BigDecimal validateAmount(BigDecimal amount) {
        requireNonNull(amount, "amount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Transaction amount must be strictly positive");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Transaction amount must have at most 2 decimal places");
        }
        return amount;
    }

    private static String normalizeNotes(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.length() > 500) {
            throw new IllegalArgumentException("Notes must not exceed 500 characters");
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}