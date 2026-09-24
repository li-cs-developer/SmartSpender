package com.smartspender.common.exception;

import com.smartspender.common.dto.ApiResponse;
import com.smartspender.common.dto.ApiResponse.FieldError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * Central translation layer: domain exceptions → HTTP responses.
 *
 * Every controller in the app benefits from this class for free.
 * Write the mapping once, use it forever.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---- 404 — resource not found ----

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.debug("Resource not found: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("NOT_FOUND", ex.getMessage()));
    }

    // ---- 400 — domain rule violations with structured codes ----

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessRule(BusinessRuleException ex) {
        log.debug("Business rule violation [{}]: {}", ex.getCode(), ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    // ---- 400 — bad business input (from our domain) ----

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        log.debug("Bad request: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("INVALID_INPUT", ex.getMessage()));
    }

    // ---- 400 — database constraint violations (safety net) ----

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        // Postgres unique constraint violations and CHECK failures land here.
        // The message is unsafe to return — it contains SQL details.
        log.warn("Data integrity violation: {}", ex.getMessage());

        String code = "DATA_INTEGRITY_VIOLATION";
        String msg = "This change violates a database constraint";

        if (ex.getMessage() != null && ex.getMessage().contains("uq_categories_user_color")) {
            code = "CATEGORY_COLOR_TAKEN";
            msg = "That color is already used by another category";
        } else if (ex.getMessage() != null && ex.getMessage().contains("uq_categories_user_name")) {
            code = "CATEGORY_NAME_TAKEN";
            msg = "A category with that name already exists";
        } else if (ex.getMessage() != null && ex.getMessage().contains("chk_transactions_amount_positive")) {
            code = "INVALID_INPUT";
            msg = "Amount must be greater than zero";
        }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(code, msg));
    }

    // ---- 400 — bean-validation failures (@Valid on request bodies) ----

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
            .toList();

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.validationError("Validation failed", fieldErrors));
    }

    // ---- 400 — malformed request body ----

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.debug("Unreadable request body: {}", ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("MALFORMED_BODY", "Request body is missing or malformed"));
    }

    // ---- 401 — missing authentication ----

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoAuth(IllegalStateException ex) {
        if (ex.getMessage() != null && ex.getMessage().contains("No authenticated user")) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("UNAUTHENTICATED", "Authentication required"));
        }
        // Any other IllegalStateException is a real bug
        log.error("Unhandled IllegalStateException", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    // ---- 500 — catch-all safety net ----

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}