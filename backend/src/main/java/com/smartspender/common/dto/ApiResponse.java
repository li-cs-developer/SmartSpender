package com.smartspender.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Uniform response envelope for every REST endpoint.
 *
 * Success:  { data: <T>, error: null, timestamp }
 * Failure:  { data: null, error: { code, message, fields? }, timestamp }
 *
 * Why the envelope? The frontend axios interceptor inspects `error` for every
 * response — no per-endpoint guessing about whether the body is a payload or
 * an error object.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    T data,
    ApiError error,
    Instant timestamp
) {

    // ---- Success factories ----

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, null, Instant.now());
    }

    public static ApiResponse<Void> noContent() {
        return new ApiResponse<>(null, null, Instant.now());
    }

    // ---- Failure factories ----

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(null, new ApiError(code, message, null), Instant.now());
    }

    public static <T> ApiResponse<T> validationError(String message, List<FieldError> fields) {
        return new ApiResponse<>(null, new ApiError("VALIDATION_FAILED", message, fields), Instant.now());
    }

    /**
     * Nested error payload. Kept as a record so JSON serialization is trivial
     * and matches what the frontend expects.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ApiError(
        String code,
        String message,
        List<FieldError> fields
    ) {}

    /** Field-level validation error — one per invalid field. */
    public record FieldError(String field, String message) {}
}