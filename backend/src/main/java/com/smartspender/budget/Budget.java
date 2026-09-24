package com.smartspender.budget;

import com.smartspender.category.Category;
import com.smartspender.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;

/**
 * A soft spending limit for one category in one calendar month.
 *
 * Encapsulation: every field is validated in the factory constructor.
 * Mutation of the limit is intent-revealing via adjustLimitTo().
 */
@Entity
@Table(
    name = "budgets",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_budgets_user_category_period",
        columnNames = {"user_id", "category_id", "month", "year"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "limit_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal limitAmount;

    @Column(nullable = false)
    private short month;   // 1..12

    @Column(nullable = false)
    private short year;    // 2000..2100

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ---- Factory constructor ----

    public Budget(User user, Category category, BigDecimal limitAmount, int month, int year) {
        this.user = requireNonNull(user, "user");
        this.category = requireNonNull(category, "category");
        this.limitAmount = validateLimit(limitAmount);
        this.month = (short) validateMonth(month);
        this.year = (short) validateYear(year);
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // ---- Intent-revealing mutation ----

    public void adjustLimitTo(BigDecimal newLimit) {
        this.limitAmount = validateLimit(newLimit);
        this.updatedAt = Instant.now();
    }

    // ---- Convenience ----

    public YearMonth period() {
        return YearMonth.of(year, month);
    }

    // ---- Domain validation ----

    private static BigDecimal validateLimit(BigDecimal amount) {
        requireNonNull(amount, "limitAmount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Budget limit must be strictly positive");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Budget limit must have at most 2 decimal places");
        }
        return amount;
    }

    private static int validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12, got " + month);
        }
        return month;
    }

    private static int validateYear(int year) {
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 2000 and 2100, got " + year);
        }
        return year;
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}