package com.smartspender.transaction;

import com.smartspender.common.period.PeriodSelection;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Immutable summary of a user's spending for a given period.
 *
 * All derived metrics are computed by the object itself — callers never
 * need to know the formulas. Both `averagePerDay` and `averagePerMonth`
 * are always available; the frontend picks which to display based on mode.
 */
public record SpendingSummary(
    BigDecimal totalExpenses,
    BigDecimal totalIncome,
    long expenseCount,      // number of EXPENSE transactions
    long incomeCount,       // number of INCOME transactions
    PeriodSelection period
) {

    public SpendingSummary {
        if (totalExpenses == null || totalExpenses.signum() < 0) {
            throw new IllegalArgumentException("totalExpenses must be non-negative");
        }
        if (totalIncome == null || totalIncome.signum() < 0) {
            throw new IllegalArgumentException("totalIncome must be non-negative");
        }
        if (expenseCount < 0) {
            throw new IllegalArgumentException("expenseCount must be >= 0");
        }
        if (incomeCount < 0) {
            throw new IllegalArgumentException("incomeCount must be >= 0");
        }
        if (period == null) {
            throw new IllegalArgumentException("period must not be null");
        }
    }

    /** Net balance: income − expenses. Can be negative. */
    public BigDecimal netFlow() {
        return totalIncome.subtract(totalExpenses);
    }

    /** Average expense per calendar day in the period. */
    public BigDecimal averagePerDay() {
        return totalExpenses.divide(
            BigDecimal.valueOf(period.totalDays()),
            2,
            RoundingMode.HALF_UP
        );
    }

    /** Average expense per month (12 for a year, or the same as totalExpenses for a single month). */
    public BigDecimal averagePerMonth() {
        long months = (period.isYear()) ? 12L : 1L;
        return totalExpenses.divide(
            BigDecimal.valueOf(months),
            2,
            RoundingMode.HALF_UP
        );
    }
}