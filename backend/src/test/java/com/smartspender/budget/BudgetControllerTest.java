package com.smartspender.budget;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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

import com.smartspender.budget.dto.BudgetResponse;
import com.smartspender.budget.dto.EnvelopeStatusResponse;
import com.smartspender.common.exception.GlobalExceptionHandler;
import com.smartspender.common.exception.ResourceNotFoundException;
import com.smartspender.config.CurrentUserArgumentResolver;
import com.smartspender.user.User;

@ExtendWith(MockitoExtension.class)
@DisplayName("BudgetController")
class BudgetControllerTest {

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_EMAIL = "tonko@smartspender.local";

    @Mock private BudgetService budgetService;

    private MockMvc mockMvc;
    private User testUser;

    @BeforeEach
    void setUp() {
        BudgetController controller = new BudgetController(budgetService);

        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        testUser = new User(TEST_EMAIL, "hashed", "Tonko");
        ReflectionTestUtils.setField(testUser, "id", TEST_USER_ID);

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
    // GET /api/budgets
    // =====================================================================

    @Nested
    @DisplayName("GET /api/budgets")
    class ListBudgets {

        @Test
        @DisplayName("returns 200 with the month's budgets")
        void shouldReturnListForMonth() throws Exception {
            BudgetResponse budget = new BudgetResponse(
                42L, 5L, "Groceries", "#22c55e", new BigDecimal("400.00"), 9, 2026);

            when(budgetService.listBudgetsForMonth(TEST_USER_ID, 2026, 9))
                .thenReturn(List.of(budget));

            mockMvc.perform(get("/api/budgets")
                    .param("year", "2026")
                    .param("month", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].categoryName").value("Groceries"))
                .andExpect(jsonPath("$.data[0].limitAmount").value(400.00));
        }
    }

    // =====================================================================
    // GET /api/budgets/envelopes
    // =====================================================================

    @Nested
    @DisplayName("GET /api/budgets/envelopes")
    class ListEnvelopes {

        @Test
        @DisplayName("returns envelope statuses with utilization already computed")
        void shouldReturnEnvelopeStatuses() throws Exception {
            EnvelopeStatusResponse ok = new EnvelopeStatusResponse(
                42L, 5L, "Groceries", "#22c55e",
                new BigDecimal("400.00"), new BigDecimal("100.00"),
                new BigDecimal("300.00"), BigDecimal.ZERO,
                new BigDecimal("0.25"), Utilization.OK, false);

            EnvelopeStatusResponse warning = new EnvelopeStatusResponse(
                42L, 5L, "Groceries", "#22c55e",
                new BigDecimal("400.00"), new BigDecimal("340.00"),
                new BigDecimal("60.00"), BigDecimal.ZERO,
                new BigDecimal("0.85"), Utilization.WARNING, true);

            EnvelopeStatusResponse exceeded = new EnvelopeStatusResponse(
                42L, 5L, "Groceries", "#22c55e",
                new BigDecimal("400.00"), new BigDecimal("500.00"),
                BigDecimal.ZERO, new BigDecimal("100.00"),
                new BigDecimal("1.25"), Utilization.EXCEEDED, true);

            when(budgetService.getEnvelopeStatuses(TEST_USER_ID, 2026, 9))
                .thenReturn(List.of(ok, warning, exceeded));

            mockMvc.perform(get("/api/budgets/envelopes")
                    .param("year", "2026")
                    .param("month", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].utilization").value("OK"))
                .andExpect(jsonPath("$.data[0].needsAttention").value(false))
                .andExpect(jsonPath("$.data[1].utilization").value("WARNING"))
                .andExpect(jsonPath("$.data[1].needsAttention").value(true))
                .andExpect(jsonPath("$.data[2].utilization").value("EXCEEDED"))
                .andExpect(jsonPath("$.data[2].overage").value(100.00));
        }
    }

    // =====================================================================
    // PUT /api/budgets
    // =====================================================================

    @Nested
    @DisplayName("PUT /api/budgets")
    class UpsertBudget {

        @Test
        @DisplayName("returns 201 with the created or updated budget")
        void shouldUpsertAndReturn201() throws Exception {
            BudgetResponse budget = new BudgetResponse(
                42L, 5L, "Groceries", "#22c55e", new BigDecimal("400.00"), 9, 2026);

            when(budgetService.setBudget(
                    eq(TEST_USER_ID), eq(5L), any(), eq(9), eq(2026)))
                .thenReturn(budget);

            String body = """
                {
                  "categoryId": 5,
                  "limitAmount": 400.00,
                  "month": 9,
                  "year": 2026
                }
                """;

            mockMvc.perform(put("/api/budgets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.data.categoryName").value("Groceries"))
                .andExpect(jsonPath("$.data.limitAmount").value(400.00));
        }

        @Test
        @DisplayName("returns 400 when the limit is not positive")
        void shouldRejectNonPositiveLimit() throws Exception {
            String body = """
                {
                  "categoryId": 5,
                  "limitAmount": -10.00,
                  "month": 9,
                  "year": 2026
                }
                """;

            mockMvc.perform(put("/api/budgets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        }

        @Test
        @DisplayName("returns 400 when the month is out of range")
        void shouldRejectBadMonth() throws Exception {
            String body = """
                {
                  "categoryId": 5,
                  "limitAmount": 100.00,
                  "month": 13,
                  "year": 2026
                }
                """;

            mockMvc.perform(put("/api/budgets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        }
    }

    // =====================================================================
    // DELETE /api/budgets/{id}
    // =====================================================================

    @Nested
    @DisplayName("DELETE /api/budgets/{id}")
    class DeleteBudget {

        @Test
        @DisplayName("returns 200 with no content when deletion succeeds")
        void shouldDeleteAndReturn200() throws Exception {
            mockMvc.perform(delete("/api/budgets/42"))
                .andExpect(status().isOk());

            verify(budgetService).deleteBudget(42L, TEST_USER_ID);
        }

        @Test
        @DisplayName("returns 404 when the budget does not belong to the user")
        void shouldReturn404WhenMissing() throws Exception {
            doThrow(ResourceNotFoundException.forId("Budget", 404L))
                .when(budgetService).deleteBudget(404L, TEST_USER_ID);

            mockMvc.perform(delete("/api/budgets/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }
    }
}