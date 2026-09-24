package com.smartspender.common.period;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Immutable value object describing "which slice of time is the user looking at?"
 *
 * Two modes:
 *   - MONTH: year + month (1..12)
 *   - YEAR:  year only
 *
 * This is the domain's language for a time period. Services accept it,
 * controllers build it from query params, and repositories never see it
 * (they get concrete `from`/`to` dates).
 *
 * Design note: a record with a discriminated `mode` field is idiomatic Java 21.
 * It avoids class-hierarchy gymnastics while still guaranteeing we cannot
 * construct an invalid state (e.g. MONTH with null month).
 */
public record PeriodSelection(
    PeriodMode mode,
    int year,
    Integer month   // null when mode == YEAR
) {

    // ---- Validation ----

    public PeriodSelection {
        if (mode == null) {
            throw new IllegalArgumentException("Period mode must not be null");
        }
        if (year < 2000 || year > 2100) {
            throw new IllegalArgumentException("Year must be between 2000 and 2100, got " + year);
        }
        if (mode == PeriodMode.MONTH) {
            if (month == null) {
                throw new IllegalArgumentException("Month is required when mode is MONTH");
            }
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("Month must be between 1 and 12, got " + month);
            }
        } else {
            if (month != null) {
                throw new IllegalArgumentException("Month must be null when mode is YEAR");
            }
        }
    }

    // ---- Factory methods ----

    public static PeriodSelection month(int year, int month) {
        return new PeriodSelection(PeriodMode.MONTH, year, month);
    }

    public static PeriodSelection year(int year) {
        return new PeriodSelection(PeriodMode.YEAR, year, null);
    }

    // ---- Range helpers ----

    /** First day of the period, inclusive. */
    public LocalDate start() {
        return (mode == PeriodMode.MONTH)
            ? LocalDate.of(year, month, 1)
            : LocalDate.of(year, 1, 1);
    }

    /** Last day of the period, inclusive. */
    public LocalDate end() {
        return (mode == PeriodMode.MONTH)
            ? YearMonth.of(year, month).atEndOfMonth()
            : LocalDate.of(year, 12, 31);
    }

    /** Number of calendar days in this period (28..31 for MONTH, 365/366 for YEAR). */
    public long totalDays() {
        return start().datesUntil(end().plusDays(1)).count();
    }

    /** Human-readable label. Used by the frontend to render the period picker. */
    public String label() {
        return (mode == PeriodMode.MONTH)
            ? String.format("%s %d", monthName(month), year)
            : String.valueOf(year);
    }

    // ---- Convenience ----

    public boolean isMonth() {
        return mode == PeriodMode.MONTH;
    }

    public boolean isYear() {
        return mode == PeriodMode.YEAR;
    }

    private static String monthName(int month) {
        return switch (month) {
            case 1  -> "January";
            case 2  -> "February";
            case 3  -> "March";
            case 4  -> "April";
            case 5  -> "May";
            case 6  -> "June";
            case 7  -> "July";
            case 8  -> "August";
            case 9  -> "September";
            case 10 -> "October";
            case 11 -> "November";
            case 12 -> "December";
            default -> throw new IllegalArgumentException("Invalid month: " + month);
        };
    }
}