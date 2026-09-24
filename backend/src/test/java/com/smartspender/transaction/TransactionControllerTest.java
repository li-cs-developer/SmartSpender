package com.smartspender.transaction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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

import com.smartspender.common.exception.GlobalExceptionHandler;
import com.smartspender.common.exception.ResourceNotFoundException;
import com.smartspender.common.period.PeriodMode;
import com.smartspender.config.CurrentUserArgumentResolver;
import com.smartspender.transaction.dto.CategorySpendingResponse;
import com.smartspender.transaction.dto.SpendingSummaryResponse;
import com.smartspender.transaction.dto.TransactionResponse;
import com.smartspender.user.User;

/**
 * Web-layer tests for TransactionController.
 *
 * Standalone MockMvc — no Spring context. The service is mocked and we
 * assert only the HTTP contract: status codes, JSON shape, headers, principal.
 *
 * The controller resolves the current user via @CurrentUserId, so we register
 * the resolver explicitly and populate SecurityContextHolder in @BeforeEach.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionController")
class TransactionControllerTest {

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_EMAIL = "tonko@smartspender.local";

    @Mock
    private TransactionService transactionService;

    private MockMvc mockMvc;
    private User testUser;

    @BeforeEach
    void setUp() {
        TransactionController controller = new TransactionController(transactionService);

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
    // GET /api/transactions
    // =====================================================================

    @Nested
    @DisplayName("GET /api/transactions")
    class ListTransactions {

        @Test
        @DisplayName("returns 200 with a wrapped list for a MONTH period")
        void shouldReturnListForMonth() throws Exception {
            var response = new TransactionResponse(
                1L,
                new BigDecimal("12.50"),
                TransactionType.EXPENSE,
                "Starbucks",                      // name
                "Morning coffee",                 // notes
                LocalDate.of(2026, 9, 15),        // date
                null, null, null                  // categoryId, categoryName, categoryColor
            );

            when(transactionService.listTransactionsForUser(eq(TEST_USER_ID), any()))
                .thenReturn(List.of(response));

            mockMvc.perform(get("/api/transactions")
                    .param("period", "MONTH")
                    .param("year", "2026")
                    .param("month", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Starbucks"))
                .andExpect(jsonPath("$.data[0].amount").value(12.50))
                .andExpect(jsonPath("$.data[0].type").value("EXPENSE"));
        }

        @Test
        @DisplayName("returns 400 when MONTH period has no month param")
        void shouldReturn400WhenMonthMissing() throws Exception {
            mockMvc.perform(get("/api/transactions")
                    .param("period", "MONTH")
                    .param("year", "2026"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
        }
    }

    // =====================================================================
    // GET /api/transactions/{id}
    // =====================================================================

    @Nested
    @DisplayName("GET /api/transactions/{id}")
    class GetTransaction {

        @Test
        @DisplayName("returns 200 with the transaction when it exists")
        void shouldReturnTransaction() throws Exception {
            var response = new TransactionResponse(
                7L,
                new BigDecimal("42.00"),
                TransactionType.EXPENSE,
                "Amazon",
                null,
                LocalDate.of(2026, 9, 10),
                null, null, null
            );

            when(transactionService.getTransactionForUser(7L, TEST_USER_ID))
                .thenReturn(response);

            mockMvc.perform(get("/api/transactions/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Amazon"));
        }

        @Test
        @DisplayName("returns 404 when the transaction does not exist")
        void shouldReturn404WhenMissing() throws Exception {
            when(transactionService.getTransactionForUser(404L, TEST_USER_ID))
                .thenThrow(ResourceNotFoundException.forId("Transaction", 404L));

            mockMvc.perform(get("/api/transactions/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }
    }

    // =====================================================================
    // POST /api/transactions
    // =====================================================================

    @Nested
    @DisplayName("POST /api/transactions")
    class CreateTransaction {

        @Test
        @DisplayName("returns 201 with Location and the created transaction")
        void shouldCreate() throws Exception {
            var response = new TransactionResponse(
                1L,
                new BigDecimal("12.50"),
                TransactionType.EXPENSE,
                "Starbucks",
                "Morning coffee",
                LocalDate.of(2026, 9, 15),
                42L, "Groceries", "#22c55e"
            );

            when(transactionService.createTransaction(
                    eq(TEST_USER_ID), eq(42L), any(), any(), any(), any(), any()))
                .thenReturn(response);

            String body = """
                {
                  "amount": 12.50,
                  "type": "EXPENSE",
                  "name": "Starbucks",
                  "notes": "Morning coffee",
                  "date": "2026-09-15",
                  "categoryId": 42
                }
                """;

            mockMvc.perform(post("/api/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.data.name").value("Starbucks"))
                .andExpect(jsonPath("$.data.type").value("EXPENSE"));
        }

        @Test
        @DisplayName("returns 400 when the name is missing")
        void shouldReturn400WhenNameMissing() throws Exception {
            String body = """
                {
                  "amount": 12.50,
                  "type": "EXPENSE",
                  "notes": "Morning coffee",
                  "date": "2026-09-15",
                  "categoryId": 42
                }
                """;

            mockMvc.perform(post("/api/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        }
    }

    // =====================================================================
    // GET /api/transactions/summary
    // =====================================================================

    @Nested
    @DisplayName("GET /api/transactions/summary")
    class Summary {

        @Test
        @DisplayName("returns the aggregated summary for a MONTH period")
        void shouldReturnMonthSummary() throws Exception {
            var response = new SpendingSummaryResponse(
                new BigDecimal("432.10"),
                new BigDecimal("2500.00"),
                new BigDecimal("2067.90"),
                8L, 4L, 30L,
                new BigDecimal("14.40"),
                new BigDecimal("432.10"),
                "September 2026", PeriodMode.MONTH, 2026, 9
            );

            when(transactionService.getSpendingSummary(eq(TEST_USER_ID), any()))
                .thenReturn(response);

            mockMvc.perform(get("/api/transactions/summary")
                    .param("period", "MONTH")
                    .param("year", "2026")
                    .param("month", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalExpenses").value(432.10))
                .andExpect(jsonPath("$.data.expenseCount").value(8))
                .andExpect(jsonPath("$.data.incomeCount").value(4))
                .andExpect(jsonPath("$.data.periodLabel").value("September 2026"))
                .andExpect(jsonPath("$.data.periodMode").value("MONTH"));
        }

        @Test
        @DisplayName("returns the aggregated summary for a YEAR period")
        void shouldReturnYearSummary() throws Exception {
            var response = new SpendingSummaryResponse(
                new BigDecimal("5200.00"),
                new BigDecimal("30000.00"),
                new BigDecimal("24800.00"),
                96L, 12L, 365L,
                new BigDecimal("14.25"),
                new BigDecimal("433.33"),
                "2026", PeriodMode.YEAR, 2026, null
            );

            when(transactionService.getSpendingSummary(eq(TEST_USER_ID), any()))
                .thenReturn(response);

            mockMvc.perform(get("/api/transactions/summary")
                    .param("period", "YEAR")
                    .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.periodLabel").value("2026"))
                .andExpect(jsonPath("$.data.periodMode").value("YEAR"))
                .andExpect(jsonPath("$.data.averagePerMonth").value(433.33));
        }
    }

    // =====================================================================
    // GET /api/transactions/by-category
    // =====================================================================

    @Nested
    @DisplayName("GET /api/transactions/by-category")
    class ByCategory {

        @Test
        @DisplayName("returns the category breakdown for EXPENSE")
        void shouldReturnByCategoryForExpense() throws Exception {
            var groceries = new CategorySpendingResponse(
                1L, "Groceries", "#22c55e", new BigDecimal("180.50"));

            when(transactionService.getSpendingByCategory(
                    eq(TEST_USER_ID), any(), eq(TransactionType.EXPENSE)))
                .thenReturn(List.of(groceries));

            mockMvc.perform(get("/api/transactions/by-category")
                    .param("period", "MONTH")
                    .param("year", "2026")
                    .param("month", "9")
                    .param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].categoryName").value("Groceries"))
                .andExpect(jsonPath("$.data[0].total").value(180.50))
                .andExpect(jsonPath("$.data[0].categoryColor").value("#22c55e"));
        }

        @Test
        @DisplayName("returns the category breakdown for INCOME")
        void shouldReturnByCategoryForIncome() throws Exception {
            var salary = new CategorySpendingResponse(
                3L, "Salary", "#22c55e", new BigDecimal("3200.00"));

            when(transactionService.getSpendingByCategory(
                    eq(TEST_USER_ID), any(), eq(TransactionType.INCOME)))
                .thenReturn(List.of(salary));

            mockMvc.perform(get("/api/transactions/by-category")
                    .param("period", "MONTH")
                    .param("year", "2026")
                    .param("month", "9")
                    .param("type", "INCOME"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].categoryName").value("Salary"))
                .andExpect(jsonPath("$.data[0].total").value(3200.00));
        }
    }

    // =====================================================================
    // GET /api/transactions/largest
    // =====================================================================

    @Nested
    @DisplayName("GET /api/transactions/largest")
    class Largest {

        @Test
        @DisplayName("returns the largest expense")
        void shouldReturnLargest() throws Exception {
            var response = new TransactionResponse(
                7L,
                new BigDecimal("450.00"),
                TransactionType.EXPENSE,
                "AirCanada",
                null,
                LocalDate.of(2026, 9, 5),
                null, null, null
            );

            when(transactionService.getLargestExpense(eq(TEST_USER_ID), any()))
                .thenReturn(Optional.of(response));

            mockMvc.perform(get("/api/transactions/largest")
                    .param("period", "MONTH")
                    .param("year", "2026")
                    .param("month", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("AirCanada"))
                .andExpect(jsonPath("$.data.amount").value(450.00));
        }

        @Test
        @DisplayName("returns null data when none exist")
        void shouldReturnNullWhenEmpty() throws Exception {
            when(transactionService.getLargestExpense(eq(TEST_USER_ID), any()))
                .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/transactions/largest")
                    .param("period", "YEAR")
                    .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
        }
    }
}