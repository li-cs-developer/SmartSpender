package com.smartspender.common.period;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PeriodSelection")
class PeriodSelectionTest {

    // =====================================================================
    // Construction
    // =====================================================================

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("creates a valid MONTH selection")
        void shouldCreateValidMonth() {
            PeriodSelection p = PeriodSelection.month(2026, 9);
            assertThat(p.mode()).isEqualTo(PeriodMode.MONTH);
            assertThat(p.year()).isEqualTo(2026);
            assertThat(p.month()).isEqualTo(9);
        }

        @Test
        @DisplayName("creates a valid YEAR selection")
        void shouldCreateValidYear() {
            PeriodSelection p = PeriodSelection.year(2026);
            assertThat(p.mode()).isEqualTo(PeriodMode.YEAR);
            assertThat(p.year()).isEqualTo(2026);
            assertThat(p.month()).isNull();
        }

        @Test
        @DisplayName("rejects MONTH mode with null month")
        void shouldRejectMonthWithoutMonth() {
            assertThatThrownBy(() -> new PeriodSelection(PeriodMode.MONTH, 2026, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Month is required");
        }

        @Test
        @DisplayName("rejects YEAR mode with a non-null month")
        void shouldRejectYearWithMonth() {
            assertThatThrownBy(() -> new PeriodSelection(PeriodMode.YEAR, 2026, 9))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Month must be null");
        }

        @Test
        @DisplayName("rejects month out of range")
        void shouldRejectInvalidMonth() {
            assertThatThrownBy(() -> PeriodSelection.month(2026, 13))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Month");

            assertThatThrownBy(() -> PeriodSelection.month(2026, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Month");
        }

        @Test
        @DisplayName("rejects year out of range")
        void shouldRejectInvalidYear() {
            assertThatThrownBy(() -> PeriodSelection.year(1999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Year");

            assertThatThrownBy(() -> PeriodSelection.year(2101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Year");
        }

        @Test
        @DisplayName("rejects null mode")
        void shouldRejectNullMode() {
            assertThatThrownBy(() -> new PeriodSelection(null, 2026, 9))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mode");
        }
    }

    // =====================================================================
    // Range helpers
    // =====================================================================

    @Nested
    @DisplayName("date range")
    class DateRange {

        @Test
        @DisplayName("month: start is the 1st, end is the last day")
        void shouldComputeMonthRange() {
            PeriodSelection p = PeriodSelection.month(2026, 9);
            assertThat(p.start()).isEqualTo(LocalDate.of(2026, 9, 1));
            assertThat(p.end()).isEqualTo(LocalDate.of(2026, 9, 30));
            assertThat(p.totalDays()).isEqualTo(30);
        }

        @Test
        @DisplayName("month: February leap year has 29 days")
        void shouldHandleLeapFebruary() {
            assertThat(PeriodSelection.month(2024, 2).totalDays()).isEqualTo(29);
            assertThat(PeriodSelection.month(2025, 2).totalDays()).isEqualTo(28);
        }

        @Test
        @DisplayName("year: start is Jan 1, end is Dec 31")
        void shouldComputeYearRange() {
            PeriodSelection p = PeriodSelection.year(2026);
            assertThat(p.start()).isEqualTo(LocalDate.of(2026, 1, 1));
            assertThat(p.end()).isEqualTo(LocalDate.of(2026, 12, 31));
            assertThat(p.totalDays()).isEqualTo(365);
        }

        @Test
        @DisplayName("year: leap year has 366 days")
        void shouldHandleLeapYear() {
            assertThat(PeriodSelection.year(2024).totalDays()).isEqualTo(366);
        }
    }

    // =====================================================================
    // Label
    // =====================================================================

    @Nested
    @DisplayName("label")
    class Label {

        @Test
        @DisplayName("month label is 'MonthName Year'")
        void shouldLabelMonth() {
            assertThat(PeriodSelection.month(2026, 9).label()).isEqualTo("September 2026");
            assertThat(PeriodSelection.month(2026, 1).label()).isEqualTo("January 2026");
            assertThat(PeriodSelection.month(2026, 12).label()).isEqualTo("December 2026");
        }

        @Test
        @DisplayName("year label is just the year")
        void shouldLabelYear() {
            assertThat(PeriodSelection.year(2026).label()).isEqualTo("2026");
        }
    }
}