package com.smartspender.transaction.dto;

import com.smartspender.common.period.PeriodMode;
import com.smartspender.transaction.SpendingSummary;

import java.math.BigDecimal;

/**
 * Outbound DTO for GET /api/transactions/summary.
 *
 * All computed values (`netFlow`, `averagePerDay`, `averagePerMonth`) are
 * pre-calculated by SpendingSummary — this record only reshapes them for JSON.
 */
public record SpendingSummaryResponse(
    BigDecimal totalExpenses,
    BigDecimal totalIncome,
    BigDecimal netFlow,
    long expenseCount,
    long incomeCount,
    long periodDays,
    BigDecimal averagePerDay,
    BigDecimal averagePerMonth,
    String periodLabel,
    PeriodMode periodMode,
    int year,
    Integer month
) {
    public static SpendingSummaryResponse from(SpendingSummary s) {
        return new SpendingSummaryResponse(
            s.totalExpenses(),
            s.totalIncome(),
            s.netFlow(),
            s.expenseCount(),
            s.incomeCount(),
            s.period().totalDays(),
            s.averagePerDay(),
            s.averagePerMonth(),
            s.period().label(),
            s.period().mode(),
            s.period().year(),
            s.period().month()
        );
    }
}
