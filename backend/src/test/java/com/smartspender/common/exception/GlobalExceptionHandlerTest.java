package com.smartspender.common.exception;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Tests for GlobalExceptionHandler.
 *
 * Uses a tiny throwaway controller that deliberately triggers each exception,
 * then asserts the handler converts it to the correct HTTP status and JSON shape.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
    }

    // ---- 404 ----

    @Test
    @DisplayName("maps ResourceNotFoundException to 404 with NOT_FOUND code")
    void shouldMapNotFoundTo404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.data").doesNotExist())
            .andExpect(jsonPath("$.error.code", is("NOT_FOUND")))
            .andExpect(jsonPath("$.error.message", containsString("Widget")));
    }

    // ---- 400 (IllegalArgument) ----

    @Test
    @DisplayName("maps IllegalArgumentException to 400 with INVALID_INPUT code")
    void shouldMapIllegalArgumentTo400() throws Exception {
        mockMvc.perform(get("/test/bad-input"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code", is("INVALID_INPUT")))
            .andExpect(jsonPath("$.error.message", containsString("from")));
    }

    // ---- 400 (validation) ----

    @Test
    @DisplayName("maps @Valid failures to 400 with field-level details")
    void shouldMapValidationErrorsTo400() throws Exception {
        mockMvc.perform(post("/test/validated")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
            .andExpect(jsonPath("$.error.fields", hasSize(1)))
            .andExpect(jsonPath("$.error.fields[0].field", is("name")));
    }

    // ---- 500 (catch-all) ----

    @Test
    @DisplayName("maps unexpected exceptions to 500 with INTERNAL_ERROR code")
    void shouldMapUnexpectedTo500() throws Exception {
        mockMvc.perform(get("/test/kaboom"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error.code", is("INTERNAL_ERROR")))
            .andExpect(jsonPath("$.error.message", is("An unexpected error occurred")));
    }

    // ---- shape ----

    @Test
    @DisplayName("includes an ISO timestamp on every error response")
    void shouldIncludeTimestamp() throws Exception {
        mockMvc.perform(get("/test/not-found"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.timestamp").exists());
    }

    // =====================================================================
    // Test-only controller
    // =====================================================================

    @RestController
    static class ThrowingController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Widget not found with id 42");
        }

        @GetMapping("/test/bad-input")
        void badInput() {
            throw new IllegalArgumentException("'from' must not be after 'to'");
        }

        @GetMapping("/test/kaboom")
        void kaboom() {
            throw new RuntimeException("boom");
        }

        @PostMapping("/test/validated")
        void validated(@Valid @RequestBody Payload payload) {
            // no-op — validation should reject empty name before body runs
        }

        record Payload(@NotBlank(message = "name is required") String name) {}
    }
}