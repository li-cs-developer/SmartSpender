package com.smartspender.budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByIdAndUserId(Long id, Long userId);

    Optional<Budget> findByUserIdAndCategoryIdAndMonthAndYear(
        Long userId, Long categoryId, int month, int year);

    List<Budget> findByUserIdAndYearAndMonthOrderByCategoryNameAsc(
        Long userId, int year, int month);

    boolean existsByUserIdAndCategoryIdAndMonthAndYear(
        Long userId, Long categoryId, int month, int year);

}