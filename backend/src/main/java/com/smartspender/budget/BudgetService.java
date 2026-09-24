package com.smartspender.budget;

import com.smartspender.budget.dto.BudgetResponse;
import com.smartspender.budget.dto.EnvelopeStatusResponse;
import com.smartspender.category.Category;
import com.smartspender.category.CategoryRepository;
import com.smartspender.common.exception.ResourceNotFoundException;
import com.smartspender.transaction.TransactionRepository;
import com.smartspender.user.User;
import com.smartspender.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;

    // =====================================================================
    // Write path
    // =====================================================================

    /** Upsert — returns a fully-materialized DTO built inside the transaction. */
    @Transactional
    public BudgetResponse setBudget(Long userId,
                                    Long categoryId,
                                    BigDecimal limitAmount,
                                    int month,
                                    int year) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> ResourceNotFoundException.forId("User", userId));

        if (!categoryRepository.existsByIdAndUserId(categoryId, userId)) {
            throw new IllegalArgumentException(
                "Category " + categoryId + " does not belong to user " + userId
            );
        }

        YearMonth.of(year, month);   // validate

        Budget budget = budgetRepository
            .findByUserIdAndCategoryIdAndMonthAndYear(userId, categoryId, month, year)
            .map(existing -> {
                existing.adjustLimitTo(limitAmount);
                return budgetRepository.save(existing);
            })
            .orElseGet(() -> {
                Category category = categoryRepository.getReferenceById(categoryId);
                Budget fresh = new Budget(user, category, limitAmount, month, year);
                return budgetRepository.save(fresh);
            });

        // Force category initialization INSIDE the transaction, then build the DTO.
        return toDto(budget);
    }

    @Transactional
    public void deleteBudget(Long budgetId, Long userId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
            .orElseThrow(() -> ResourceNotFoundException.forId("Budget", budgetId));
        budgetRepository.delete(budget);
    }

    // =====================================================================
    // Read path
    // =====================================================================

    @Transactional(readOnly = true)
    public List<BudgetResponse> listBudgetsForMonth(Long userId, int year, int month) {
        validateMonth(month);
        return budgetRepository
            .findByUserIdAndYearAndMonthOrderByCategoryNameAsc(userId, year, month)
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudget(Long budgetId, Long userId) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
            .orElseThrow(() -> ResourceNotFoundException.forId("Budget", budgetId));
        return toDto(budget);
    }

    @Transactional(readOnly = true)
    public List<EnvelopeStatusResponse> getEnvelopeStatuses(Long userId, int year, int month) {
        validateMonth(month);

        List<Budget> budgets = budgetRepository
            .findByUserIdAndYearAndMonthOrderByCategoryNameAsc(userId, year, month);

        YearMonth period = YearMonth.of(year, month);
        LocalDate from = period.atDay(1);
        LocalDate to = period.atEndOfMonth();

        return budgets.stream()
            .map(budget -> {
                // Category is touched inside the transaction — safe.
                Long categoryId = budget.getCategory().getId();
                BigDecimal spent = transactionRepository.sumExpensesByCategoryRawForBudget(
                    userId, categoryId, from, to
                );
                return EnvelopeStatusResponse.from(EnvelopeStatus.of(budget, spent));
            })
            .toList();
    }

    @Transactional
    public int copyFrom(Long userId, int fromYear, int fromMonth, int toYear, int toMonth) {
        validateMonth(fromMonth);
        validateMonth(toMonth);

        if (fromYear == toYear && fromMonth == toMonth) {
            throw new IllegalArgumentException("Source and target period must differ");
        }

        List<Budget> source = budgetRepository
            .findByUserIdAndYearAndMonthOrderByCategoryNameAsc(userId, fromYear, fromMonth);

        if (source.isEmpty()) {
            return 0;
        }

        int copied = 0;
        for (Budget b : source) {
            boolean exists = budgetRepository
                .existsByUserIdAndCategoryIdAndMonthAndYear(
                    userId, b.getCategory().getId(), toMonth, toYear);
            if (exists) continue;

            Budget fresh = new Budget(
                b.getUser(),
                b.getCategory(),
                b.getLimitAmount(),
                toMonth,
                toYear
            );
            budgetRepository.save(fresh);
            copied++;
        }
        return copied;
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    /** Materializes the lazy Category while the session is still open, then builds the DTO. */
    private BudgetResponse toDto(Budget budget) {
        // Touch the lazy fields INSIDE the transaction:
        budget.getCategory().getName();
        budget.getCategory().getColor();
        return BudgetResponse.from(budget);
    }

    private static void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException(
                "Month must be between 1 and 12, got " + month);
        }
    }
}