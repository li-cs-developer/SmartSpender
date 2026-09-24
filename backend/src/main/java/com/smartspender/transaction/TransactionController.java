package com.smartspender.transaction;

import com.smartspender.common.dto.ApiResponse;
import com.smartspender.common.period.PeriodMode;
import com.smartspender.common.period.PeriodSelection;
import com.smartspender.config.CurrentUserId;
import com.smartspender.transaction.dto.CategorySpendingResponse;
import com.smartspender.transaction.dto.SpendingSummaryResponse;
import com.smartspender.transaction.dto.TransactionRequest;
import com.smartspender.transaction.dto.TransactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // ---- Read ----

    @GetMapping
    public ApiResponse<List<TransactionResponse>> list(
            @CurrentUserId Long userId,
            @RequestParam PeriodMode period,
            @RequestParam int year,
            @RequestParam(required = false) Integer month) {
        return ApiResponse.success(
            transactionService.listTransactionsForUser(
                userId, buildPeriod(period, year, month)));
    }

    @GetMapping("/{id}")
    public ApiResponse<TransactionResponse> getOne(
            @CurrentUserId Long userId,
            @PathVariable Long id) {
        return ApiResponse.success(transactionService.getTransactionForUser(id, userId));
    }

    // ---- Write ----

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> create(
            @CurrentUserId Long userId,
            @Valid @RequestBody TransactionRequest request) {

        TransactionResponse body = transactionService.createTransaction(
            userId,
            request.categoryId(),
            request.amount(),
            request.type(),
            request.name(),
            request.notes(),
            request.date()
        );

        return ResponseEntity
            .created(URI.create("/api/transactions/" + body.id()))
            .body(ApiResponse.success(body));
    }

    // ---- Aggregations ----

    @GetMapping("/summary")
    public ApiResponse<SpendingSummaryResponse> summary(
            @CurrentUserId Long userId,
            @RequestParam PeriodMode period,
            @RequestParam int year,
            @RequestParam(required = false) Integer month) {
        return ApiResponse.success(
            transactionService.getSpendingSummary(userId, buildPeriod(period, year, month)));
    }

    @GetMapping("/by-category")
    public ApiResponse<List<CategorySpendingResponse>> byCategory(
            @CurrentUserId Long userId,
            @RequestParam PeriodMode period,
            @RequestParam int year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "EXPENSE") TransactionType type) {
        return ApiResponse.success(
            transactionService.getSpendingByCategory(
                userId, buildPeriod(period, year, month), type));
    }
    
    @GetMapping("/largest")
    public ApiResponse<TransactionResponse> largest(
            @CurrentUserId Long userId,
            @RequestParam PeriodMode period,
            @RequestParam int year,
            @RequestParam(required = false) Integer month) {
        return transactionService
            .getLargestExpense(userId, buildPeriod(period, year, month))
            .map(ApiResponse::success)
            .orElseGet(() -> ApiResponse.success(null));
    }

    // ---- Helpers ----

    /** Converts query params into a validated PeriodSelection. Throws 400 on invalid input. */
    private static PeriodSelection buildPeriod(PeriodMode mode, int year, Integer month) {
        if (mode == PeriodMode.MONTH) {
            if (month == null) {
                throw new IllegalArgumentException("Month is required when period is MONTH");
            }
            return PeriodSelection.month(year, month);
        }
        return PeriodSelection.year(year);
    }
}