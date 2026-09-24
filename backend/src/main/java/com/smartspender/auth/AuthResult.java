package com.smartspender.auth;

/**
 * Immutable result of a successful register/login.
 * Returned by AuthService — the controller then wraps it in an ApiResponse.
 */
public record AuthResult(String token, Long userId, String email, String displayName) {}