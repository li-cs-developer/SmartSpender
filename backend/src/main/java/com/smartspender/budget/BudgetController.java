package com.smartspender.budget;

import com.smartspender.budget.dto.BudgetRequest;
import com.smartspender.budget.dto.BudgetResponse;
import com.smartspender.budget.dto.EnvelopeStatusResponse;
import com.smartspender.common.dto.ApiResponse;
import com.smartspender.config.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ApiResponse<List<BudgetResponse>> list(
            @CurrentUserId Long userId,
            @RequestParam int year,
            @RequestParam int month) {
        return ApiResponse.success(budgetService.listBudgetsForMonth(userId, year, month));
    }

    @GetMapping("/{id}")
    public ApiResponse<BudgetResponse> getOne(
            @CurrentUserId Long userId,
            @PathVariable Long id) {
        return ApiResponse.success(budgetService.getBudget(id, userId));
    }

    @GetMapping("/envelopes")
    public ApiResponse<List<EnvelopeStatusResponse>> envelopes(
            @CurrentUserId Long userId,
            @RequestParam int year,
            @RequestParam int month) {
        return ApiResponse.success(budgetService.getEnvelopeStatuses(userId, year, month));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<BudgetResponse>> upsert(
            @CurrentUserId Long userId,
            @Valid @RequestBody BudgetRequest request) {

        BudgetResponse body = budgetService.setBudget(
            userId,
            request.categoryId(),
            request.limitAmount(),
            request.month(),
            request.year()
        );

        return ResponseEntity
            .created(URI.create("/api/budgets/" + body.id()))
            .body(ApiResponse.success(body));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUserId Long userId, @PathVariable Long id) {
        budgetService.deleteBudget(id, userId);
        return ApiResponse.noContent();
    }

    @PostMapping("/copy-from")
    public ApiResponse<Integer> copyFrom(
            @CurrentUserId Long userId,
            @RequestParam int fromYear,
            @RequestParam int fromMonth,
            @RequestParam int toYear,
            @RequestParam int toMonth) {
        int copied = budgetService.copyFrom(userId, fromYear, fromMonth, toYear, toMonth);
        return ApiResponse.success(copied);
    }
}