package com.smartspender.budget.dto;

import com.smartspender.budget.EnvelopeStatus;
import com.smartspender.budget.Utilization;

import java.math.BigDecimal;

/**
 * Outbound DTO for GET /api/budgets/envelopes.
 *
 * The `utilization` field carries the classification computed by the domain
 * (Utilization.OK / WARNING / EXCEEDED). The frontend maps it to a CSS class
 * and does NOT re-implement the 80%/100% threshold rules.
 */
public record EnvelopeStatusResponse(
    Long budgetId,
    Long categoryId,
    String categoryName,
    String categoryColor,
    BigDecimal limitAmount,
    BigDecimal spent,
    BigDecimal remaining,
    BigDecimal overage,
    BigDecimal percentUsed,
    Utilization utilization,
    boolean needsAttention
) {
    public static EnvelopeStatusResponse from(EnvelopeStatus status) {
        var budget = status.budget();
        return new EnvelopeStatusResponse(
            budget.getId(),
            budget.getCategory().getId(),
            budget.getCategory().getName(),
            budget.getCategory().getColor(),
            budget.getLimitAmount(),
            status.spent(),
            status.remaining(),
            status.overage(),
            status.percentUsed(),
            status.utilization(),
            status.needsAttention()
        );
    }
}