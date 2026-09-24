package com.smartspender.transaction.dto;

import com.smartspender.transaction.Transaction;
import com.smartspender.transaction.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Outbound payload — never expose entities directly. */
public record TransactionResponse(
    Long id,
    BigDecimal amount,
    TransactionType type,
    String name,
    String notes,
    LocalDate date,
    Long categoryId,
    String categoryName,
    String categoryColor
) {
    public static TransactionResponse from(Transaction tx) {
        Long catId = tx.getCategory() == null ? null : tx.getCategory().getId();
        String catName = tx.getCategory() == null ? null : tx.getCategory().getName();
        String catColor = tx.getCategory() == null ? null : tx.getCategory().getColor();

        return new TransactionResponse(
            tx.getId(),
            tx.getAmount(),
            tx.getType(),
            tx.getName(),
            tx.getNotes(),
            tx.getDate(),
            catId,
            catName,
            catColor
        );
    }
}