package com.smartspender.category;

import com.smartspender.category.dto.CategoryRequest;
import com.smartspender.category.dto.CategoryResponse;
import com.smartspender.common.dto.ApiResponse;
import com.smartspender.config.CurrentUserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<CategoryResponse>> list(@CurrentUserId Long userId) {
        List<CategoryResponse> payload = categoryService.listCategoriesForUser(userId).stream()
            .map(CategoryResponse::from)
            .toList();
        return ApiResponse.success(payload);
    }

    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getOne(
            @CurrentUserId Long userId,
            @PathVariable Long id) {
        Category category = categoryService.getCategoryForUser(id, userId);
        return ApiResponse.success(CategoryResponse.from(category));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @CurrentUserId Long userId,
            @Valid @RequestBody CategoryRequest request) {

        Category created = categoryService.createCategory(
            userId, request.name(), request.icon(), request.color()
        );

        return ResponseEntity
            .created(URI.create("/api/categories/" + created.getId()))
            .body(ApiResponse.success(CategoryResponse.from(created)));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(
            @CurrentUserId Long userId,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {

        Category updated = categoryService.updateCategory(
            id, userId, request.name(), request.icon(), request.color()
        );
        return ApiResponse.success(CategoryResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @CurrentUserId Long userId,
            @PathVariable Long id) {
        categoryService.deleteCategory(id, userId);
        return ResponseEntity.noContent().build();
    }
}