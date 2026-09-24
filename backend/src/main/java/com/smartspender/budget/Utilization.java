package com.smartspender.budget;

import java.math.BigDecimal;

/**
 * How close a budget envelope is to being exhausted.
 *
 * Thresholds are business rules, not presentation:
 *   - OK       : under 80%
 *   - WARNING  : 80% to 99.99%
 *   - EXCEEDED : 100% or more
 *
 * The UI maps these to colors. If the thresholds ever change (e.g. move
 * WARNING to 75%), this enum changes in ONE place and every consumer follows.
 */
public enum Utilization {

    OK,
    WARNING,
    EXCEEDED;

    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("0.80");
    private static final BigDecimal EXCEEDED_THRESHOLD = BigDecimal.ONE;

    /**
     * Classifies spending against a limit.
     *
     * @param limit positive budget amount
     * @param spent non-negative amount already spent
     */
    public static Utilization classify(BigDecimal limit, BigDecimal spent) {
        if (limit == null || limit.signum() <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        if (spent == null || spent.signum() < 0) {
            throw new IllegalArgumentException("spent must be non-negative");
        }

        BigDecimal ratio = spent.divide(limit, 4, java.math.RoundingMode.HALF_UP);

        if (ratio.compareTo(EXCEEDED_THRESHOLD) >= 0) return EXCEEDED;
        if (ratio.compareTo(WARNING_THRESHOLD) >= 0) return WARNING;
        return OK;
    }
}