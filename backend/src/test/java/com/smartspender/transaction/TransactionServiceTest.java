package com.smartspender.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.smartspender.category.Category;
import com.smartspender.category.CategoryRepository;
import com.smartspender.common.exception.BusinessRuleException;
import com.smartspender.common.exception.ResourceNotFoundException;
import com.smartspender.common.period.PeriodSelection;
import com.smartspender.transaction.dto.CategorySpendingResponse;
import com.smartspender.transaction.dto.SpendingSummaryResponse;
import com.smartspender.transaction.dto.TransactionResponse;
import com.smartspender.user.User;
import com.smartspender.user.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService")
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private UserRepository userRepository;
    @Mock private CategoryRepository categoryRepository;

    @InjectMocks private TransactionService transactionService;

    private User testUser;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testUser = new User("tonko@smartspender.local", "hashed", "Tonko");
        ReflectionTestUtils.setField(testUser, "id", 1L);
        testCategory = new Category(testUser, "Groceries", "pi pi-shopping-cart", "#22c55e");
        ReflectionTestUtils.setField(testCategory, "id", 42L);
    }

    // =====================================================================
    // Read path
    // =====================================================================

    @Nested
    @DisplayName("listTransactionsForUser")
    class ListTransactionsForUser {

        @Test
        @DisplayName("returns transactions in the given month")
        void shouldListTransactionsForMonth() {
            Long userId = 1L;
            PeriodSelection period = PeriodSelection.month(2026, 9);

            Transaction coffee = new Transaction(testUser, null, new BigDecimal("4.75"),
                TransactionType.EXPENSE, "Starbucks", null, LocalDate.of(2026, 9, 15));
            Transaction groceries = new Transaction(testUser, null, new BigDecimal("82.30"),
                TransactionType.EXPENSE, "Loblaws", null, LocalDate.of(2026, 9, 20));

            when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDescIdDesc(
                    userId, period.start(), period.end()))
                .thenReturn(List.of(groceries, coffee));

            List<TransactionResponse> result =
                transactionService.listTransactionsForUser(userId, period);

            assertThat(result).hasSize(2)
                .extracting(TransactionResponse::name)
                .containsExactly("Loblaws", "Starbucks");
        }

        @Test
        @DisplayName("returns an empty list when no transactions exist in the period")
        void shouldReturnEmptyList() {
            Long userId = 1L;
            PeriodSelection period = PeriodSelection.year(2020);

            when(transactionRepository.findByUserIdAndDateBetweenOrderByDateDescIdDesc(
                    userId, period.start(), period.end()))
                .thenReturn(List.of());

            assertThat(transactionService.listTransactionsForUser(userId, period)).isEmpty();
        }
    }

    @Nested
    @DisplayName("getTransactionForUser")
    class GetTransactionForUser {

        @Test
        @DisplayName("returns the transaction when owned by user")
        void shouldReturnWhenOwned() {
            Transaction tx = new Transaction(testUser, null, new BigDecimal("42.00"),
                TransactionType.EXPENSE, "Amazon", null, LocalDate.of(2026, 9, 10));
            ReflectionTestUtils.setField(tx, "id", 7L);

            when(transactionRepository.findByIdAndUserId(7L, 1L)).thenReturn(Optional.of(tx));

            assertThat(transactionService.getTransactionForUser(7L, 1L).name())
                .isEqualTo("Amazon");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when not found")
        void shouldThrowWhenNotFound() {
            when(transactionRepository.findByIdAndUserId(404L, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.getTransactionForUser(404L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
        }
    }

    // =====================================================================
    // Create path
    // =====================================================================

    @Nested
    @DisplayName("createTransaction")
    class CreateTransaction {

        @Test
        @DisplayName("creates an EXPENSE when all inputs are valid")
        void shouldCreateExpense() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            TransactionResponse created = transactionService.createTransaction(
                1L, null, new BigDecimal("12.50"), TransactionType.EXPENSE,
                "Starbucks", "Morning coffee", LocalDate.of(2026, 9, 15)
            );

            assertThat(created.name()).isEqualTo("Starbucks");
            assertThat(created.amount()).isEqualByComparingTo("12.50");
            assertThat(created.type()).isEqualTo(TransactionType.EXPENSE);
        }

        @Test
        @DisplayName("attaches a category when owned by user")
        void shouldAttachCategory() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(categoryRepository.existsByIdAndUserId(42L, 1L)).thenReturn(true);
            when(categoryRepository.getReferenceById(42L)).thenReturn(testCategory);
            when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            TransactionResponse created = transactionService.createTransaction(
                1L, 42L, new BigDecimal("55.00"), TransactionType.EXPENSE,
                "Loblaws", null, LocalDate.of(2026, 9, 20)
            );

            assertThat(created.categoryId()).isEqualTo(42L);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when user missing")
        void shouldRejectUnknownUser() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> transactionService.createTransaction(
                999L, null, new BigDecimal("10.00"), TransactionType.EXPENSE,
                "Store", null, LocalDate.of(2026, 9, 15)))
                .isInstanceOf(ResourceNotFoundException.class);

            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BusinessRuleException when category belongs to another user")
        void shouldRejectForeignCategory() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(categoryRepository.existsByIdAndUserId(42L, 1L)).thenReturn(false);

            assertThatThrownBy(() -> transactionService.createTransaction(
                1L, 42L, new BigDecimal("55.00"), TransactionType.EXPENSE,
                "Loblaws", null, LocalDate.of(2026, 9, 20)))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("CATEGORY_NOT_OWNED");
        }
    }

    // =====================================================================
    // getSpendingSummary — period-aware
    // =====================================================================

    @Nested
    @DisplayName("getSpendingSummary")
    class GetSpendingSummary {

        @Test
        @DisplayName("aggregates a MONTH period into a summary")
        void shouldAggregateMonth() {
            Long userId = 1L;
            PeriodSelection period = PeriodSelection.month(2026, 9);

            when(transactionRepository.sumExpensesInRange(userId, period.start(), period.end()))
                .thenReturn(new BigDecimal("432.10"));
            when(transactionRepository.sumIncomeInRange(userId, period.start(), period.end()))
                .thenReturn(new BigDecimal("2500.00"));
            when(transactionRepository.countExpensesInRange(userId, period.start(), period.end()))
                .thenReturn(8L);
            when(transactionRepository.countIncomesInRange(userId, period.start(), period.end()))
                .thenReturn(4L);

            SpendingSummaryResponse summary =
                transactionService.getSpendingSummary(userId, period);

            assertThat(summary.totalExpenses()).isEqualByComparingTo("432.10");
            assertThat(summary.totalIncome()).isEqualByComparingTo("2500.00");
            assertThat(summary.expenseCount()).isEqualTo(8);
            assertThat(summary.incomeCount()).isEqualTo(4);
            assertThat(summary.netFlow()).isEqualByComparingTo("2067.90");
            assertThat(summary.periodDays()).isEqualTo(30);
            assertThat(summary.periodLabel()).isEqualTo("September 2026");
            assertThat(summary.periodMode()).isEqualTo(com.smartspender.common.period.PeriodMode.MONTH);
        }

        @Test
        @DisplayName("computes averagePerDay against the actual period length")
        void shouldComputeAveragePerDay() {
            Long userId = 1L;
            PeriodSelection period = PeriodSelection.month(2026, 9); // 30 days

            when(transactionRepository.sumExpensesInRange(userId, period.start(), period.end()))
                .thenReturn(new BigDecimal("300.00"));
            when(transactionRepository.sumIncomeInRange(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.countExpensesInRange(any(), any(), any())).thenReturn(3L);
            when(transactionRepository.countIncomesInRange(any(), any(), any())).thenReturn(0L);

            SpendingSummaryResponse summary =
                transactionService.getSpendingSummary(userId, period);

            assertThat(summary.averagePerDay()).isEqualByComparingTo("10.00");
        }

        @Test
        @DisplayName("computes averagePerMonth as total/12 for a YEAR period")
        void shouldComputeAveragePerMonth() {
            Long userId = 1L;
            PeriodSelection period = PeriodSelection.year(2026);

            when(transactionRepository.sumExpensesInRange(userId, period.start(), period.end()))
                .thenReturn(new BigDecimal("1200.00"));
            when(transactionRepository.sumIncomeInRange(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
            when(transactionRepository.countExpensesInRange(any(), any(), any())).thenReturn(24L);
            when(transactionRepository.countIncomesInRange(any(), any(), any())).thenReturn(0L);

            SpendingSummaryResponse summary =
                transactionService.getSpendingSummary(userId, period);

            assertThat(summary.averagePerMonth()).isEqualByComparingTo("100.00");
            assertThat(summary.periodDays()).isEqualTo(365);
            assertThat(summary.periodLabel()).isEqualTo("2026");
        }

        @Test
        @DisplayName("returns zeroed summary when there is no data")
        void shouldReturnZeroed() {
            Long userId = 1L;
            PeriodSelection period = PeriodSelection.month(2026, 9);

            when(transactionRepository.sumExpensesInRange(any(), any(), any())).thenReturn(BigDecimal.ZERO);
            when(transactionRepository.sumIncomeInRange(any(), any(), any())).thenReturn(BigDecimal.ZERO);
            when(transactionRepository.countExpensesInRange(any(), any(), any())).thenReturn(0L);
            when(transactionRepository.countIncomesInRange(any(), any(), any())).thenReturn(0L);

            SpendingSummaryResponse summary =
                transactionService.getSpendingSummary(userId, period);

            assertThat(summary.totalExpenses()).isEqualByComparingTo("0.00");
            assertThat(summary.expenseCount()).isZero();
        }
    }

    // =====================================================================
    // getSpendingByCategory
    // =====================================================================

    @Nested
    @DisplayName("getSpendingByCategory")
    class GetSpendingByCategory {

        @Test
        @DisplayName("maps raw projections to DTOs for EXPENSE")
        void shouldMapToDtosForExpense() {
            Long userId = 1L;
            PeriodSelection period = PeriodSelection.month(2026, 9);

            Object[] groceriesRow = { 1L, "Groceries", "#22c55e", new BigDecimal("180.50") };

            when(transactionRepository.sumByCategoryAndType(
                    userId, TransactionType.EXPENSE, period.start(), period.end()))
                .thenReturn(List.<Object[]>of(groceriesRow));

            List<CategorySpendingResponse> result = transactionService
                .getSpendingByCategory(userId, period, TransactionType.EXPENSE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).categoryName()).isEqualTo("Groceries");
            assertThat(result.get(0).total()).isEqualByComparingTo("180.50");
        }

        @Test
        @DisplayName("maps raw projections to DTOs for INCOME")
        void shouldMapToDtosForIncome() {
            Long userId = 1L;
            PeriodSelection period = PeriodSelection.month(2026, 9);

            Object[] salaryRow = { 3L, "Salary", "#22c55e", new BigDecimal("3200.00") };

            when(transactionRepository.sumByCategoryAndType(
                    userId, TransactionType.INCOME, period.start(), period.end()))
                .thenReturn(List.<Object[]>of(salaryRow));

            List<CategorySpendingResponse> result = transactionService
                .getSpendingByCategory(userId, period, TransactionType.INCOME);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).categoryName()).isEqualTo("Salary");
            assertThat(result.get(0).total()).isEqualByComparingTo("3200.00");
        }

        @Test
        @DisplayName("returns empty list when no data")
        void shouldReturnEmpty() {
            Long userId = 1L;
            PeriodSelection period = PeriodSelection.year(2020);

            when(transactionRepository.sumByCategoryAndType(
                    userId, TransactionType.EXPENSE, period.start(), period.end()))
                .thenReturn(List.of());

            assertThat(transactionService
                .getSpendingByCategory(userId, period, TransactionType.EXPENSE))
                .isEmpty();
        }
    }

    // =====================================================================
    // getLargestExpense
    // =====================================================================

    @Nested
    @DisplayName("getLargestExpense")
    class GetLargestExpense {

        @Test
        @DisplayName("returns the largest expense DTO")
        void shouldReturnLargest() {
            Long userId = 1L;
            PeriodSelection period = PeriodSelection.month(2026, 9);

            Transaction big = new Transaction(testUser, null, new BigDecimal("450.00"),
                TransactionType.EXPENSE, "AirCanada", null, LocalDate.of(2026, 9, 5));
            ReflectionTestUtils.setField(big, "id", 7L);

            when(transactionRepository.findLargestExpenseInRange(
                    eq(userId), eq(period.start()), eq(period.end()), any(Pageable.class)))
                .thenReturn(List.of(big));

            Optional<TransactionResponse> result =
                transactionService.getLargestExpense(userId, period);

            assertThat(result).isPresent();
            assertThat(result.get().name()).isEqualTo("AirCanada");
        }

        @Test
        @DisplayName("returns empty when no expenses exist")
        void shouldReturnEmpty() {
            Long userId = 1L;
            PeriodSelection period = PeriodSelection.month(2026, 9);

            when(transactionRepository.findLargestExpenseInRange(
                    eq(userId), eq(period.start()), eq(period.end()), any(Pageable.class)))
                .thenReturn(List.of());

            assertThat(transactionService.getLargestExpense(userId, period)).isEmpty();
        }
    }
}