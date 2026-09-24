package com.smartspender.common.exception;

/**
 * Base for all domain rule violations that should map to a 4xx response
 * with a machine-readable code.
 *
 * The `code` field is what the frontend uses to look up a friendly message.
 * The `message` is what developers see in logs and debugging — it can be
 * verbose.
 *
 * Subclasses should NOT use this directly. Two concrete subclasses exist:
 *   - BusinessRuleException  → HTTP 400
 *   - ResourceNotFoundException (already exists) → HTTP 404
 */
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}