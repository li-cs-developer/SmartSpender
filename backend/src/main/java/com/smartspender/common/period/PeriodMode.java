package com.smartspender.common.period;

/**
 * The two ways a user can slice time on the dashboard.
 *
 * MONTH — one calendar month (e.g. September 2026)
 * YEAR  — one calendar year (e.g. 2026)
 *
 * Kept as an enum so switch statements are exhaustive and the JSON
 * serialization is a stable string ("MONTH" / "YEAR").
 */
public enum PeriodMode {
    MONTH,
    YEAR
}