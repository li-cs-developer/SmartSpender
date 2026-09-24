package com.smartspender.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // ---- Read path ----

    List<Transaction> findByUserIdAndDateBetweenOrderByDateDescIdDesc(
        Long userId, LocalDate from, LocalDate to);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    /** Used by CategoryService.deleteCategory to guard against orphaning transactions. */
    boolean existsByCategoryId(Long categoryId);

    // ---- Aggregations ----

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = 'EXPENSE'
          AND t.date BETWEEN :from AND :to
        """)
    BigDecimal sumExpensesInRange(
        @Param("userId") Long userId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = 'INCOME'
          AND t.date BETWEEN :from AND :to
        """)
    BigDecimal sumIncomeInRange(
        @Param("userId") Long userId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to);

    @Query("""
        SELECT COUNT(t)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = 'EXPENSE'
          AND t.date BETWEEN :from AND :to
        """)
    long countExpensesInRange(
        @Param("userId") Long userId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to);

    @Query("""
        SELECT COUNT(t)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = 'INCOME'
          AND t.date BETWEEN :from AND :to
        """)
    long countIncomesInRange(
        @Param("userId") Long userId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to);

    /**
     * Sum of amounts grouped by category, filtered by type.
     * Used for both the expense donut and the income donut.
     *
     * `type` is bound as a {@link TransactionType} enum so JPQL compares
     * the entity's enum-mapped field directly, without relying on string
     * coercion that could silently fail if the column is stored as an
     * ordinal or a non-matching string.
     */
    @Query("""
        SELECT c.id, c.name, c.color, SUM(t.amount)
        FROM Transaction t
        JOIN t.category c
        WHERE t.user.id = :userId
          AND t.type = :type
          AND t.date BETWEEN :from AND :to
        GROUP BY c.id, c.name, c.color
        ORDER BY SUM(t.amount) DESC
        """)
    List<Object[]> sumByCategoryAndType(
        @Param("userId") Long userId,
        @Param("type") TransactionType type,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to);

    @Query("""
        SELECT t
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.type = 'EXPENSE'
          AND t.date BETWEEN :from AND :to
        ORDER BY t.amount DESC, t.id ASC
        """)
    List<Transaction> findLargestExpenseInRange(
        @Param("userId") Long userId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to,
        org.springframework.data.domain.Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
          AND t.category.id = :categoryId
          AND t.type = 'EXPENSE'
          AND t.date BETWEEN :from AND :to
        """)
    BigDecimal sumExpensesByCategoryRawForBudget(
        @Param("userId") Long userId,
        @Param("categoryId") Long categoryId,
        @Param("from") LocalDate from,
        @Param("to") LocalDate to);
}