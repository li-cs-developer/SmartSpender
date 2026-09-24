package com.smartspender.category;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.smartspender.common.exception.BusinessRuleException;
import com.smartspender.common.exception.GlobalExceptionHandler;
import com.smartspender.common.exception.ResourceNotFoundException;
import com.smartspender.config.CurrentUserArgumentResolver;
import com.smartspender.user.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryController")
class CategoryControllerTest {

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_EMAIL = "tonko@smartspender.local";

    @Mock
    private CategoryService categoryService;

    private MockMvc mockMvc;
    private User testUser;
    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        CategoryController controller = new CategoryController(categoryService);

        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        testUser = new User(TEST_EMAIL, "hashed", "Tonko");
        ReflectionTestUtils.setField(testUser, "id", TEST_USER_ID);

        sampleCategory = new Category(testUser, "Groceries", "pi pi-shopping-cart", "#22c55e");
        ReflectionTestUtils.setField(sampleCategory, "id", 7L);

        Authentication auth = new UsernamePasswordAuthenticationToken(
            testUser, null, AuthorityUtils.createAuthorityList("ROLE_USER")
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // =====================================================================
    // GET /api/categories
    // =====================================================================

    @Nested
    @DisplayName("GET /api/categories")
    class ListCategories {

        @Test
        @DisplayName("returns 200 with the user's categories")
        void shouldReturnList() throws Exception {
            when(categoryService.listCategoriesForUser(TEST_USER_ID))
                .thenReturn(List.of(sampleCategory));

            mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("Groceries"))
                .andExpect(jsonPath("$.data[0].color").value("#22c55e"))
                .andExpect(jsonPath("$.data[0].isDefault").value(false));
        }
    }

    // =====================================================================
    // GET /api/categories/{id}
    // =====================================================================

    @Nested
    @DisplayName("GET /api/categories/{id}")
    class GetCategory {

        @Test
        @DisplayName("returns 200 when the category belongs to the user")
        void shouldReturnCategory() throws Exception {
            when(categoryService.getCategoryForUser(7L, TEST_USER_ID))
                .thenReturn(sampleCategory);

            mockMvc.perform(get("/api/categories/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Groceries"));
        }

        @Test
        @DisplayName("returns 404 when the category does not exist")
        void shouldReturn404WhenMissing() throws Exception {
            when(categoryService.getCategoryForUser(404L, TEST_USER_ID))
                .thenThrow(ResourceNotFoundException.forId("Category", 404L));

            mockMvc.perform(get("/api/categories/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }
    }

    // =====================================================================
    // POST /api/categories
    // =====================================================================

    @Nested
    @DisplayName("POST /api/categories")
    class CreateCategory {

        @Test
        @DisplayName("returns 201 with a Location header and the created category")
        void shouldCreateAndReturn201() throws Exception {
            when(categoryService.createCategory(
                    eq(TEST_USER_ID), any(), any(), any()))
                .thenReturn(sampleCategory);

            String body = """
                {
                  "name": "Groceries",
                  "icon": "pi pi-shopping-cart",
                  "color": "#22c55e"
                }
                """;

            mockMvc.perform(post("/api/categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.data.name").value("Groceries"));
        }

        @Test
        @DisplayName("returns 400 when the color is not a valid hex string")
        void shouldRejectBadColor() throws Exception {
            String body = """
                {
                  "name": "Groceries",
                  "icon": "pi pi-shopping-cart",
                  "color": "green"
                }
                """;

            mockMvc.perform(post("/api/categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("returns 400 when the name is missing")
        void shouldRejectMissingName() throws Exception {
            String body = """
                {
                  "icon": "pi pi-shopping-cart",
                  "color": "#22c55e"
                }
                """;

            mockMvc.perform(post("/api/categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("returns 400 with CATEGORY_NAME_TAKEN when the name is a duplicate")
        void shouldReturn400OnDuplicateName() throws Exception {
            when(categoryService.createCategory(
                    eq(TEST_USER_ID), any(), any(), any()))
                .thenThrow(BusinessRuleException.categoryNameTaken("Groceries"));

            String body = """
                {
                  "name": "Groceries",
                  "icon": "pi pi-shopping-cart",
                  "color": "#22c55e"
                }
                """;

            mockMvc.perform(post("/api/categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NAME_TAKEN"));
        }

        @Test
        @DisplayName("returns 400 with CATEGORY_COLOR_TAKEN when the color is a duplicate")
        void shouldReturn400OnDuplicateColor() throws Exception {
            when(categoryService.createCategory(
                    eq(TEST_USER_ID), any(), any(), any()))
                .thenThrow(BusinessRuleException.categoryColorTaken("#22c55e"));

            String body = """
                {
                  "name": "Fresh",
                  "icon": "pi pi-tag",
                  "color": "#22c55e"
                }
                """;

            mockMvc.perform(post("/api/categories")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_COLOR_TAKEN"));
        }
    }

    // =====================================================================
    // PUT /api/categories/{id}
    // =====================================================================

    @Nested
    @DisplayName("PUT /api/categories/{id}")
    class UpdateCategory {

        @Test
        @DisplayName("returns 200 with the updated category")
        void shouldUpdateAndReturn200() throws Exception {
            when(categoryService.updateCategory(
                    eq(7L), eq(TEST_USER_ID), any(), any(), any()))
                .thenReturn(sampleCategory);

            String body = """
                {
                  "name": "Food",
                  "icon": "pi pi-apple",
                  "color": "#3b82f6"
                }
                """;

            mockMvc.perform(put("/api/categories/7")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Groceries"));
        }

        @Test
        @DisplayName("returns 400 with CATEGORY_NAME_TAKEN when the rename collides")
        void shouldReturn400OnRenameCollision() throws Exception {
            when(categoryService.updateCategory(
                    eq(7L), eq(TEST_USER_ID), any(), any(), any()))
                .thenThrow(BusinessRuleException.categoryNameTaken("Dining"));

            String body = """
                {
                  "name": "Dining",
                  "icon": "pi pi-tag",
                  "color": "#3b82f6"
                }
                """;

            mockMvc.perform(put("/api/categories/7")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_NAME_TAKEN"));
        }

        @Test
        @DisplayName("returns 404 when updating a category that does not exist")
        void shouldReturn404WhenUpdatingMissingCategory() throws Exception {
            when(categoryService.updateCategory(
                    eq(404L), eq(TEST_USER_ID), any(), any(), any()))
                .thenThrow(ResourceNotFoundException.forId("Category", 404L));

            String body = """
                {
                  "name": "Food",
                  "icon": "pi pi-tag",
                  "color": "#3b82f6"
                }
                """;

            mockMvc.perform(put("/api/categories/404")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }
    }

    // =====================================================================
    // DELETE /api/categories/{id}
    // =====================================================================

    @Nested
    @DisplayName("DELETE /api/categories/{id}")
    class DeleteCategory {

        @Test
        @DisplayName("returns 204 when the category is deleted")
        void shouldDeleteAndReturn204() throws Exception {
            mockMvc.perform(delete("/api/categories/7"))
                .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("returns 400 with CATEGORY_IN_USE when transactions reference it")
        void shouldReturn400WhenInUse() throws Exception {
            doThrow(BusinessRuleException.categoryInUse())
                .when(categoryService).deleteCategory(7L, TEST_USER_ID);

            mockMvc.perform(delete("/api/categories/7"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CATEGORY_IN_USE"));
        }

        @Test
        @DisplayName("returns 404 when deleting a category that does not exist")
        void shouldReturn404WhenDeletingMissingCategory() throws Exception {
            doThrow(ResourceNotFoundException.forId("Category", 404L))
                .when(categoryService).deleteCategory(404L, TEST_USER_ID);

            mockMvc.perform(delete("/api/categories/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }
    }
}