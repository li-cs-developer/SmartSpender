package com.smartspender.budget.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Payload for PUT /api/budgets — creates or updates the limit for
 * a (user, category, month, year) tuple.
 */
public record BudgetRequest(

    @NotNull(message = "categoryId is required")
    Long categoryId,

    @NotNull(message = "limitAmount is required")
    @DecimalMin(value = "0.01", message = "limitAmount must be greater than 0")
    BigDecimal limitAmount,

    @NotNull(message = "month is required")
    @Min(value = 1, message = "month must be between 1 and 12")
    @Max(value = 12, message = "month must be between 1 and 12")
    Integer month,

    @NotNull(message = "year is required")
    @Min(value = 2000, message = "year must be between 2000 and 2100")
    @Max(value = 2100, message = "year must be between 2000 and 2100")
    Integer year
) {}