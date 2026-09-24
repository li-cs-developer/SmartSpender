package com.smartspender.transaction.dto;

import com.smartspender.transaction.CategorySpending;

import java.math.BigDecimal;

public record CategorySpendingResponse(
    Long categoryId,
    String categoryName,
    String categoryColor,
    BigDecimal total
) {
    public static CategorySpendingResponse from(CategorySpending c) {
        return new CategorySpendingResponse(
            c.categoryId(), c.categoryName(), c.categoryColor(), c.total()
        );
    }
}