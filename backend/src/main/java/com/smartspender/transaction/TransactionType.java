package com.smartspender.transaction;

/**
 * Whether a transaction increases or decreases the user's balance.
 * Kept as an enum (not a boolean) so we can extend with TRANSFER, REFUND, etc. later.
 */
public enum TransactionType {
    EXPENSE,
    INCOME
}