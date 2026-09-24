package com.smartspender.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartspender.budget.dto.BudgetResponse;
import com.smartspender.budget.dto.EnvelopeStatusResponse;
import com.smartspender.category.Category;
import com.smartspender.category.CategoryRepository;
import com.smartspender.common.exception.ResourceNotFoundException;
import com.smartspender.transaction.TransactionRepository;
import com.smartspender.user.User;
import com.smartspender.user.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("BudgetService")
class BudgetServiceTest {

    @Mock private BudgetRepository budgetRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks private BudgetService budgetService;

    private User testUser;
    private Category groceries;

    @BeforeEach
    void setUp() {
        testUser = new User("tonko@smartspender.local", "hashed", "Tonko");
        org.springframework.test.util.ReflectionTestUtils.setField(testUser, "id", 1L);
        groceries = new Category(testUser, "Groceries", "pi pi-shopping-cart", "#22c55e");
        org.springframework.test.util.ReflectionTestUtils.setField(groceries, "id", 5L);
    }

    // =====================================================================
    // setBudget
    // =====================================================================

    @Nested
    @DisplayName("setBudget")
    class SetBudget {

        @Test
        @DisplayName("creates a new budget when none exists for the user/category/month")
        void shouldCreateNewBudget() {
            Long userId = 1L;
            Long categoryId = 5L;

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(categoryRepository.existsByIdAndUserId(categoryId, userId)).thenReturn(true);
            when(categoryRepository.getReferenceById(categoryId)).thenReturn(groceries);
            when(budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(userId, categoryId, 9, 2026))
                .thenReturn(Optional.empty());
            when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            BudgetResponse budget = budgetService.setBudget(
                userId, categoryId, new BigDecimal("400.00"), 9, 2026);

            assertThat(budget.categoryName()).isEqualTo("Groceries");
            assertThat(budget.categoryColor()).isEqualTo("#22c55e");
            assertThat(budget.limitAmount()).isEqualByComparingTo("400.00");
            assertThat(budget.month()).isEqualTo(9);
            assertThat(budget.year()).isEqualTo(2026);
        }

        @Test
        @DisplayName("updates the limit when a budget already exists for the period")
        void shouldUpdateExistingBudget() {
            Long userId = 1L;
            Long categoryId = 5L;
            Budget existing = new Budget(testUser, groceries, new BigDecimal("300.00"), 9, 2026);

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(categoryRepository.existsByIdAndUserId(categoryId, userId)).thenReturn(true);
            when(budgetRepository.findByUserIdAndCategoryIdAndMonthAndYear(userId, categoryId, 9, 2026))
                .thenReturn(Optional.of(existing));
            when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            BudgetResponse budget = budgetService.setBudget(
                userId, categoryId, new BigDecimal("500.00"), 9, 2026);

            assertThat(budget.limitAmount()).isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when the user does not exist")
        void shouldRejectUnknownUser() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.setBudget(
                999L, 5L, new BigDecimal("100.00"), 9, 2026))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");

            verify(budgetRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws IllegalArgumentException when the category belongs to another user")
        void shouldRejectForeignCategory() {
            Long userId = 1L;
            Long foreignCategoryId = 99L;

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(categoryRepository.existsByIdAndUserId(foreignCategoryId, userId)).thenReturn(false);

            assertThatThrownBy(() -> budgetService.setBudget(
                userId, foreignCategoryId, new BigDecimal("100.00"), 9, 2026))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category");

            verify(budgetRepository, never()).save(any());
        }
    }

    // =====================================================================
    // getEnvelopeStatuses
    // =====================================================================

    @Nested
    @DisplayName("getEnvelopeStatuses")
    class GetEnvelopeStatuses {

        @Test
        @DisplayName("returns one EnvelopeStatusResponse per budget with correct Utilization")
        void shouldReturnEnvelopeStatusPerBudget() {
            Long userId = 1L;

            Budget b1 = new Budget(testUser, groceries, new BigDecimal("400.00"), 9, 2026);
            Budget b2 = new Budget(testUser, groceries, new BigDecimal("200.00"), 9, 2026);

            when(budgetRepository.findByUserIdAndYearAndMonthOrderByCategoryNameAsc(userId, 2026, 9))
                .thenReturn(List.of(b1, b2));

            // b1 spent 100 / 400 = 25%  -> OK
            // b2 spent 250 / 200 = 125% -> EXCEEDED
            when(transactionRepository.sumExpensesByCategoryRawForBudget(any(), any(), any(), any()))
                .thenReturn(new BigDecimal("100.00"), new BigDecimal("250.00"));

            List<EnvelopeStatusResponse> statuses =
                budgetService.getEnvelopeStatuses(userId, 2026, 9);

            assertThat(statuses).hasSize(2);
            assertThat(statuses.get(0).utilization()).isEqualTo(Utilization.OK);
            assertThat(statuses.get(1).utilization()).isEqualTo(Utilization.EXCEEDED);
        }
    }

    // =====================================================================
    // listBudgetsForMonth
    // =====================================================================

    @Nested
    @DisplayName("listBudgetsForMonth")
    class ListBudgetsForMonth {

        @Test
        @DisplayName("returns budgets for the given user/year/month")
        void shouldReturnBudgetsForPeriod() {
            Long userId = 1L;
            Budget b = new Budget(testUser, groceries, new BigDecimal("400.00"), 9, 2026);

            when(budgetRepository.findByUserIdAndYearAndMonthOrderByCategoryNameAsc(userId, 2026, 9))
                .thenReturn(List.of(b));

            List<BudgetResponse> result = budgetService.listBudgetsForMonth(userId, 2026, 9);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).categoryName()).isEqualTo("Groceries");
        }

        @Test
        @DisplayName("throws IllegalArgumentException for an out-of-range month")
        void shouldRejectInvalidMonth() {
            assertThatThrownBy(() -> budgetService.listBudgetsForMonth(1L, 2026, 13))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Month");
        }
    }

    // =====================================================================
    // deleteBudget
    // =====================================================================

    @Nested
    @DisplayName("deleteBudget")
    class DeleteBudget {

        @Test
        @DisplayName("deletes the budget when it belongs to the user")
        void shouldDeleteWhenOwned() {
            Long userId = 1L;
            Budget b = new Budget(testUser, groceries, new BigDecimal("400.00"), 9, 2026);

            when(budgetRepository.findByIdAndUserId(7L, userId)).thenReturn(Optional.of(b));

            budgetService.deleteBudget(7L, userId);

            verify(budgetRepository).delete(b);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when the budget does not belong to the user")
        void shouldRejectForeignBudget() {
            when(budgetRepository.findByIdAndUserId(7L, 2L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> budgetService.deleteBudget(7L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);

            verify(budgetRepository, never()).delete(any());
        }
    }

    // =====================================================================
    // copyFrom
    // =====================================================================

    @Nested
    @DisplayName("copyFrom")
    class CopyFrom {

        @Test
        @DisplayName("copies budgets from one period to another, skipping duplicates")
        void shouldCopyAndSkipDuplicates() {
            Long userId = 1L;
            Budget august = new Budget(testUser, groceries, new BigDecimal("400.00"), 8, 2026);

            when(budgetRepository.findByUserIdAndYearAndMonthOrderByCategoryNameAsc(userId, 2026, 8))
                .thenReturn(List.of(august));
            when(budgetRepository.existsByUserIdAndCategoryIdAndMonthAndYear(userId, 5L, 9, 2026))
                .thenReturn(false);
            when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            int copied = budgetService.copyFrom(userId, 2026, 8, 2026, 9);

            assertThat(copied).isEqualTo(1);
            verify(budgetRepository).save(any(Budget.class));
        }

        @Test
        @DisplayName("returns 0 when the source period has no budgets")
        void shouldReturnZeroWhenSourceEmpty() {
            Long userId = 1L;
            when(budgetRepository.findByUserIdAndYearAndMonthOrderByCategoryNameAsc(userId, 2026, 7))
                .thenReturn(List.of());

            int copied = budgetService.copyFrom(userId, 2026, 7, 2026, 8);

            assertThat(copied).isZero();
            verify(budgetRepository, never()).save(any());
        }
    }
}