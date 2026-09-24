package com.smartspender.budget;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Immutable snapshot of one envelope's state: budget + spent + derived metrics.
 *
 * This is the "Tell, Don't Ask" poster child. Instead of the controller or the
 * Vue component computing:
 *
 *     const ratio = spent / limit;
 *     const color = ratio >= 1 ? 'red' : ratio >= 0.8 ? 'amber' : 'green';
 *     const remaining = limit - spent;
 *
 * the domain does it once, here, with tests. The frontend just reads fields.
 */
public record EnvelopeStatus(
    Budget budget,
    BigDecimal spent,
    Utilization utilization
) {

    public EnvelopeStatus {
        if (budget == null) {
            throw new IllegalArgumentException("budget must not be null");
        }
        if (spent == null || spent.signum() < 0) {
            throw new IllegalArgumentException("spent must be non-negative");
        }
        if (utilization == null) {
            throw new IllegalArgumentException("utilization must not be null");
        }
    }

    /** Factory: derives the Utilization from the budget and spent amount. */
    public static EnvelopeStatus of(Budget budget, BigDecimal spent) {
        BigDecimal normalized = spent == null ? BigDecimal.ZERO : spent;
        Utilization level = Utilization.classify(budget.getLimitAmount(), normalized);
        return new EnvelopeStatus(budget, normalized, level);
    }

    // ---- Derived metrics ----

    /** Amount still available before hitting the limit. Never negative. */
    public BigDecimal remaining() {
        BigDecimal raw = budget.getLimitAmount().subtract(spent);
        return raw.signum() < 0 ? BigDecimal.ZERO : raw;
    }

    /** Amount spent above the limit. Zero if not over. */
    public BigDecimal overage() {
        BigDecimal raw = spent.subtract(budget.getLimitAmount());
        return raw.signum() > 0 ? raw : BigDecimal.ZERO;
    }

    /** Fraction of the budget used, e.g. 0.8234 for 82.34%. Always >= 0. */
    public BigDecimal percentUsed() {
        return spent.divide(budget.getLimitAmount(), 4, RoundingMode.HALF_UP);
    }

    public boolean isExceeded() {
        return utilization == Utilization.EXCEEDED;
    }

    public boolean needsAttention() {
        return utilization == Utilization.WARNING || utilization == Utilization.EXCEEDED;
    }
}