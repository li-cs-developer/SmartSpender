package com.smartspender.transaction.dto;

import com.smartspender.transaction.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    BigDecimal amount,

    @NotNull(message = "type is required")
    TransactionType type,

    @NotBlank(message = "name is required")
    @Size(max = 150, message = "name must be 150 characters or fewer")
    String name,

    @Size(max = 500, message = "notes must be 500 characters or fewer")
    String notes,

    @NotNull(message = "date is required")
    LocalDate date,

    @NotNull(message = "categoryId is required")
    Long categoryId            // ← was: Long categoryId (no annotation)
) {}