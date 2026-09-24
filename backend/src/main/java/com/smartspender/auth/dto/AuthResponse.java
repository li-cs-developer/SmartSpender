package com.smartspender.auth.dto;

import com.smartspender.auth.AuthResult;

public record AuthResponse(String token, Long userId, String email, String displayName) {
    public static AuthResponse from(AuthResult r) {
        return new AuthResponse(r.token(), r.userId(), r.email(), r.displayName());
    }
}