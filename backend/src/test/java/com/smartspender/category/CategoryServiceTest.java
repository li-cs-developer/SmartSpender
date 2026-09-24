package com.smartspender.category;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.smartspender.common.exception.BusinessRuleException;
import com.smartspender.common.exception.ResourceNotFoundException;
import com.smartspender.transaction.TransactionRepository;
import com.smartspender.user.User;
import com.smartspender.user.UserRepository;

/**
 * Unit tests for CategoryService.
 *
 * Pure logic tests — no Spring context, no database.
 * Repositories are mocked, so we test ONLY the business rules.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryService")
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("tonko@smartspender.local", "hashed", "Tonko");
    }

    // =====================================================================
    // listCategoriesForUser
    // =====================================================================

    @Nested
    @DisplayName("listCategoriesForUser")
    class ListCategoriesForUser {

        @Test
        @DisplayName("returns all categories owned by the given user")
        void shouldReturnAllCategoriesForUser() {
            // given
            Long userId = 1L;
            Category groceries = new Category(testUser, "Groceries", "pi pi-shopping-cart", "#22c55e");
            Category dining = new Category(testUser, "Dining", "pi pi-star", "#f97316");

            when(categoryRepository.findByUserIdOrderByNameAsc(userId))
                .thenReturn(List.of(dining, groceries));

            // when
            List<Category> result = categoryService.listCategoriesForUser(userId);

            // then
            assertThat(result)
                .hasSize(2)
                .extracting(Category::getName)
                .containsExactly("Dining", "Groceries");
            verify(categoryRepository).findByUserIdOrderByNameAsc(userId);
        }

        @Test
        @DisplayName("returns an empty list when the user has no categories")
        void shouldReturnEmptyListWhenUserHasNoCategories() {
            when(categoryRepository.findByUserIdOrderByNameAsc(99L)).thenReturn(List.of());

            List<Category> result = categoryService.listCategoriesForUser(99L);

            assertThat(result).isEmpty();
        }
    }

    // =====================================================================
    // getCategoryForUser
    // =====================================================================

    @Nested
    @DisplayName("getCategoryForUser")
    class GetCategoryForUser {

        @Test
        @DisplayName("returns the category when it belongs to the user")
        void shouldReturnCategoryWhenFound() {
            Long userId = 1L;
            Category groceries = new Category(testUser, "Groceries", "pi pi-shopping-cart", "#22c55e");

            when(categoryRepository.findByIdAndUserId(7L, userId))
                .thenReturn(Optional.of(groceries));

            Category result = categoryService.getCategoryForUser(7L, userId);

            assertThat(result.getName()).isEqualTo("Groceries");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when the category does not exist")
        void shouldThrowWhenCategoryNotFound() {
            when(categoryRepository.findByIdAndUserId(404L, 1L))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getCategoryForUser(404L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("404");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when the category belongs to another user")
        void shouldThrowWhenCategoryBelongsToAnotherUser() {
            when(categoryRepository.findByIdAndUserId(7L, 2L))
                .thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getCategoryForUser(7L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =====================================================================
    // createCategory
    // =====================================================================

    @Nested
    @DisplayName("createCategory")
    class CreateCategory {

        @Test
        @DisplayName("saves a new category when the name is unique for the user")
        void shouldCreateCategoryWhenNameIsUnique() {
            Long userId = 1L;

            when(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "Groceries"))
                .thenReturn(false);
            when(categoryRepository.existsByUserIdAndColor(userId, "#22c55e"))
                .thenReturn(false);
            when(userRepository.findById(userId))
                .thenReturn(Optional.of(testUser));
            when(categoryRepository.save(any(Category.class)))
                .thenAnswer(inv -> inv.getArgument(0));

            Category created = categoryService.createCategory(
                userId, "Groceries", "pi pi-shopping-cart", "#22c55e"
            );

            assertThat(created.getName()).isEqualTo("Groceries");
            assertThat(created.getUser()).isEqualTo(testUser);
            verify(categoryRepository).save(any(Category.class));
        }

        @Test
        @DisplayName("throws BusinessRuleException when a category with the same name already exists")
        void shouldRejectDuplicateNameForSameUser() {
            Long userId = 1L;

            when(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "Groceries"))
                .thenReturn(true);

            assertThatThrownBy(() -> categoryService.createCategory(
                userId, "Groceries", "pi pi-shopping-cart", "#22c55e"
            ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Groceries");

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("treats category names case-insensitively (Dining vs dining)")
        void shouldRejectDuplicateIgnoringCase() {
            when(categoryRepository.existsByUserIdAndNameIgnoreCase(1L, "dining"))
                .thenReturn(true);

            assertThatThrownBy(() -> categoryService.createCategory(
                1L, "dining", "pi pi-star", "#f97316"
            ))
                .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    @DisplayName("createCategory — color uniqueness")
    class CreateCategoryColorUniqueness {

        @Test
        @DisplayName("rejects a color that is already used by another category")
        void shouldRejectDuplicateColor() {
            Long userId = 1L;
            when(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "New Category"))
                .thenReturn(false);
            when(categoryRepository.existsByUserIdAndColor(userId, "#22c55e"))
                .thenReturn(true);

            assertThatThrownBy(() -> categoryService.createCategory(
                userId, "New Category", "pi pi-tag", "#22c55e"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("#22c55e");

            verify(categoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("uses CATEGORY_NAME_TAKEN code when name duplicates")
        void shouldReturnNameTakenCode() {
            Long userId = 1L;
            when(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "Groceries"))
                .thenReturn(true);

            assertThatThrownBy(() -> categoryService.createCategory(
                userId, "Groceries", "pi pi-tag", "#999999"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("CATEGORY_NAME_TAKEN");
        }

        @Test
        @DisplayName("uses CATEGORY_COLOR_TAKEN code when color duplicates")
        void shouldReturnColorTakenCode() {
            Long userId = 1L;
            when(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "Fresh"))
                .thenReturn(false);
            when(categoryRepository.existsByUserIdAndColor(userId, "#22c55e"))
                .thenReturn(true);

            assertThatThrownBy(() -> categoryService.createCategory(
                userId, "Fresh", "pi pi-tag", "#22c55e"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("CATEGORY_COLOR_TAKEN");
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategory {

        @Test
        @DisplayName("renames and restyles a category")
        void shouldUpdateCategory() {
            Long userId = 1L;
            Category existing = new Category(testUser, "Groceries",
                "pi pi-shopping-cart", "#22c55e");
            ReflectionTestUtils.setField(existing, "id", 10L);

            when(categoryRepository.findByIdAndUserId(10L, userId))
                .thenReturn(Optional.of(existing));
            when(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "Food"))
                .thenReturn(false);
            when(categoryRepository.existsByUserIdAndColor(userId, "#3b82f6"))
                .thenReturn(false);

            Category updated = categoryService.updateCategory(
                10L, userId, "Food", "pi pi-apple", "#3b82f6");

            assertThat(updated.getName()).isEqualTo("Food");
            assertThat(updated.getColor()).isEqualTo("#3b82f6");
            assertThat(updated.getIcon()).isEqualTo("pi pi-apple");
        }

        @Test
        @DisplayName("rejects a rename that collides with another category")
        void shouldRejectRenameCollision() {
            Long userId = 1L;
            Category existing = new Category(testUser, "Groceries",
                "pi pi-shopping-cart", "#22c55e");
            ReflectionTestUtils.setField(existing, "id", 10L);

            when(categoryRepository.findByIdAndUserId(10L, userId))
                .thenReturn(Optional.of(existing));
            when(categoryRepository.existsByUserIdAndNameIgnoreCase(userId, "Dining"))
                .thenReturn(true);

            assertThatThrownBy(() -> categoryService.updateCategory(
                10L, userId, "Dining", null, null))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("CATEGORY_NAME_TAKEN");
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategory {

        @Test
        @DisplayName("deletes when the category has no transactions")
        void shouldDelete() {
            Long userId = 1L;
            Category existing = new Category(testUser, "Groceries",
                "pi pi-shopping-cart", "#22c55e");
            ReflectionTestUtils.setField(existing, "id", 10L);

            when(categoryRepository.findByIdAndUserId(10L, userId))
                .thenReturn(Optional.of(existing));
            when(transactionRepository.existsByCategoryId(10L)).thenReturn(false);

            categoryService.deleteCategory(10L, userId);

            verify(categoryRepository).delete(existing);
        }

        @Test
        @DisplayName("refuses to delete when transactions exist")
        void shouldRefuseWhenInUse() {
            Long userId = 1L;
            Category existing = new Category(testUser, "Groceries",
                "pi pi-shopping-cart", "#22c55e");
            ReflectionTestUtils.setField(existing, "id", 10L);

            when(categoryRepository.findByIdAndUserId(10L, userId))
                .thenReturn(Optional.of(existing));
            when(transactionRepository.existsByCategoryId(10L)).thenReturn(true);

            assertThatThrownBy(() -> categoryService.deleteCategory(10L, userId))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo("CATEGORY_IN_USE");

            verify(categoryRepository, never()).delete(any());
        }
    }
}