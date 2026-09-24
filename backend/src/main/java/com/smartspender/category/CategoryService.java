package com.smartspender.category;

import com.smartspender.common.exception.BusinessRuleException;
import com.smartspender.common.exception.ResourceNotFoundException;
import com.smartspender.transaction.TransactionRepository;
import com.smartspender.user.User;
import com.smartspender.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<Category> listCategoriesForUser(Long userId) {
        return categoryRepository.findByUserIdOrderByNameAsc(userId);
    }

    @Transactional(readOnly = true)
    public Category getCategoryForUser(Long categoryId, Long userId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
            .orElseThrow(() -> ResourceNotFoundException.forId("Category", categoryId));
    }

    /**
     * Creates a new category for the given user.
     *
     * Design note: takes userId (not User) — the service only needs identity,
     * not the full aggregate. Enforces two per-user uniqueness rules:
     *   1. Name must be unique (case-insensitive).
     *   2. Color must be unique.
     * Both rules are also enforced by DB constraints; the DB is the source of
     * truth, and these checks give the client a friendly, structured error.
     */
    @Transactional
    public Category createCategory(Long userId, String name, String icon, String color) {
        String trimmedName = name == null ? "" : name.trim();

        // Name uniqueness — friendly error
        if (categoryRepository.existsByUserIdAndNameIgnoreCase(userId, trimmedName)) {
            throw BusinessRuleException.categoryNameTaken(trimmedName);
        }

        // Color uniqueness — friendly error
        if (categoryRepository.existsByUserIdAndColor(userId, color)) {
            throw BusinessRuleException.categoryColorTaken(color);
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> ResourceNotFoundException.forId("User", userId));

        Category category = new Category(user, trimmedName, icon, color);
        return categoryRepository.save(category);
    }

    /**
     * Updates name, icon, and/or color of an existing category.
     *
     * Only non-null fields are applied. Uniqueness checks run only when the
     * corresponding field is actually changing, so a no-op save doesn't
     * collide with the row's own current name/color.
     */
    @Transactional
    public Category updateCategory(Long categoryId,
                                   Long userId,
                                   String newName,
                                   String newIcon,
                                   String newColor) {

        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
            .orElseThrow(() -> ResourceNotFoundException.forId("Category", categoryId));

        // If the name is changing, check it's still unique for this user
        if (newName != null && !newName.trim().equalsIgnoreCase(category.getName())) {
            String trimmed = newName.trim();
            if (categoryRepository.existsByUserIdAndNameIgnoreCase(userId, trimmed)) {
                throw BusinessRuleException.categoryNameTaken(trimmed);
            }
            category.renameTo(trimmed);
        }

        // If the color is changing, check it's still unique
        if (newColor != null && !newColor.equals(category.getColor())) {
            if (categoryRepository.existsByUserIdAndColor(userId, newColor)) {
                throw BusinessRuleException.categoryColorTaken(newColor);
            }
        }

        // Restyle handles icon + color together
        String icon = newIcon != null ? newIcon : category.getIcon();
        String color = newColor != null ? newColor : category.getColor();
        category.restyle(icon, color);

        return category;
    }

    /**
     * Deletes a category, refusing if any transaction still references it.
     *
     * The DB has ON DELETE RESTRICT as the ultimate guard; the explicit
     * check here produces a friendly, structured error instead of a raw
     * DataIntegrityViolationException when the delete would cascade.
     */
    @Transactional
    public void deleteCategory(Long categoryId, Long userId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
            .orElseThrow(() -> ResourceNotFoundException.forId("Category", categoryId));

        if (transactionRepository.existsByCategoryId(categoryId)) {
            throw BusinessRuleException.categoryInUse();
        }

        categoryRepository.delete(category);
    }
}