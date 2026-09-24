package com.smartspender.transaction;

import java.math.BigDecimal;

/**
 * Translates the raw Object[] projections returned by the repository into
 * the strongly-typed CategorySpending value object.
 *
 * Kept separate from the service so the projection-shape knowledge lives
 * in exactly one place. If the JPQL query changes column order, only this
 * class needs to change.
 */
final class CategorySpendingMapper {

    private CategorySpendingMapper() {
        // utility class — no instances
    }

    static CategorySpending fromRow(Object[] row) {
        Long id       = ((Number) row[0]).longValue();
        String name   = (String) row[1];
        String color  = (String) row[2];
        BigDecimal total = (BigDecimal) row[3];
        return new CategorySpending(id, name, color, total);
    }
}