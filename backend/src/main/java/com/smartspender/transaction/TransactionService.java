package com.smartspender.transaction;

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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    // =====================================================================
    // Read path
    // =====================================================================

    @Transactional(readOnly = true)
    public List<TransactionResponse> listTransactionsForUser(Long userId, PeriodSelection period) {
        return transactionRepository
            .findByUserIdAndDateBetweenOrderByDateDescIdDesc(
                userId, period.start(), period.end())
            .stream()
            .map(this::toDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransactionForUser(Long transactionId, Long userId) {
        Transaction tx = transactionRepository.findByIdAndUserId(transactionId, userId)
            .orElseThrow(() -> ResourceNotFoundException.forId("Transaction", transactionId));
        return toDto(tx);
    }

    // =====================================================================
    // Write path
    // =====================================================================

    @Transactional
    public TransactionResponse createTransaction(Long userId,
                                                 Long categoryId,
                                                 BigDecimal amount,
                                                 TransactionType type,
                                                 String merchant,
                                                 String notes,
                                                 LocalDate date) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> ResourceNotFoundException.forId("User", userId));

        Category category = resolveCategory(categoryId, userId);

        Transaction transaction = new Transaction(
            user, category, amount, type, merchant, notes, date
        );
        Transaction saved = transactionRepository.save(transaction);
        return toDto(saved);
    }

    // =====================================================================
    // Aggregations
    // =====================================================================

    @Transactional(readOnly = true)
    public SpendingSummaryResponse getSpendingSummary(Long userId, PeriodSelection period) {
        LocalDate from = period.start();
        LocalDate to   = period.end();

        BigDecimal expenses   = normalize(transactionRepository.sumExpensesInRange(userId, from, to));
        BigDecimal income     = normalize(transactionRepository.sumIncomeInRange(userId, from, to));
        long expenseCount     = transactionRepository.countExpensesInRange(userId, from, to);
        long incomeCount      = transactionRepository.countIncomesInRange(userId, from, to);

        return SpendingSummaryResponse.from(
            new SpendingSummary(expenses, income, expenseCount, incomeCount, period)
        );
    }

    @Transactional(readOnly = true)
    public List<CategorySpendingResponse> getSpendingByCategory(
            Long userId, PeriodSelection period, TransactionType type) {
        return transactionRepository
            .sumByCategoryAndType(userId, type, period.start(), period.end())
            .stream()
            .map(CategorySpendingMapper::fromRow)
            .map(CategorySpendingResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TransactionResponse> getLargestExpense(Long userId, PeriodSelection period) {
        List<Transaction> results = transactionRepository.findLargestExpenseInRange(
            userId, period.start(), period.end(), PageRequest.of(0, 1)
        );
        return results.isEmpty()
            ? Optional.empty()
            : Optional.of(toDto(results.get(0)));
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private TransactionResponse toDto(Transaction tx) {
        if (tx.getCategory() != null) {
            tx.getCategory().getName();
            tx.getCategory().getColor();
        }
        return TransactionResponse.from(tx);
    }

    private Category resolveCategory(Long categoryId, Long userId) {
        if (categoryId == null) return null;
        if (!categoryRepository.existsByIdAndUserId(categoryId, userId)) {
            throw BusinessRuleException.categoryNotOwned(categoryId);   // ← was IllegalArgumentException
        }
        return categoryRepository.getReferenceById(categoryId);
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}