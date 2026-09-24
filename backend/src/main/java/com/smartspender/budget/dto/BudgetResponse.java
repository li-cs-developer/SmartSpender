package com.smartspender.budget.dto;

import com.smartspender.budget.Budget;

import java.math.BigDecimal;

public record BudgetResponse(
    Long id,
    Long categoryId,
    String categoryName,
    String categoryColor,
    BigDecimal limitAmount,
    int month,
    int year
) {
    public static BudgetResponse from(Budget b) {
        return new BudgetResponse(
            b.getId(),
            b.getCategory().getId(),
            b.getCategory().getName(),
            b.getCategory().getColor(),
            b.getLimitAmount(),
            b.getMonth(),
            b.getYear()
        );
    }
}