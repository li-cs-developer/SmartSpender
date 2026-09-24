package com.smartspender.transaction;

import java.math.BigDecimal;

/**
 * Immutable snapshot of how much was spent in one category over a date range.
 *
 * A record is the ideal home for a value object: no identity, immutable,
 * equals/hashCode/toString free.
 *
 * Note: `total` is always positive (spending is expressed as a positive number).
 * The "it's an expense" information comes from the context, not the sign.
 */
public record CategorySpending(
    Long categoryId,
    String categoryName,
    String categoryColor,
    BigDecimal total
) {
    public CategorySpending {
        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("categoryName must not be blank");
        }
        if (total == null || total.signum() < 0) {
            throw new IllegalArgumentException("total must be non-negative");
        }
    }
}