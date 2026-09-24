package com.smartspender.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

/**
 * Unit tests for JwtService.
 *
 * No Spring context — we test the cryptographic primitives directly.
 * This is the kind of code where TDD pays for itself: every edge case you can
 * think of (expired, tampered, wrong signature) gets a dedicated test.
 */
@DisplayName("JwtService")
class JwtServiceTest {

    private static final String SECRET =
        "dev-only-test-secret-key-must-be-at-least-32-bytes-long-for-hs256";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000L); // 1 minute for tests
    }

    @Test
    @DisplayName("generates a token that starts with 'eyJ' (JWT header)")
    void shouldGenerateWellFormedToken() {
        String token = jwtService.generateToken(42L, "tonko@smartspender.local");

        assertThat(token)
            .isNotBlank()
            .startsWith("eyJ")  // base64url of '{"' — the standard JWT header prefix
            .contains(".");     // three segments: header.payload.signature
    }

    @Test
    @DisplayName("extracts userId and email from a freshly issued token")
    void shouldExtractClaimsFromValidToken() {
        String token = jwtService.generateToken(42L, "tonko@smartspender.local");

        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.extractEmail(token)).isEqualTo("tonko@smartspender.local");
    }

    @Test
    @DisplayName("rejects an expired token")
    void shouldRejectExpiredToken() {
        JwtService shortLived = new JwtService(SECRET, -1_000L); // already expired
        String token = shortLived.generateToken(42L, "tonko@smartspender.local");

        assertThatThrownBy(() -> shortLived.extractUserId(token))
            .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("rejects a token signed with a different key")
    void shouldRejectTamperedSignature() {
        String token = jwtService.generateToken(42L, "tonko@smartspender.local");

        JwtService impostor = new JwtService(
            "another-secret-of-sufficient-length-for-hs256-algorithm",
            60_000L
        );

        assertThatThrownBy(() -> impostor.extractUserId(token))
            .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("rejects a key that is too short for the configured algorithm")
    void shouldRejectWeakKey() {
        assertThatThrownBy(() -> new JwtService("too-short", 60_000L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("rejects a malformed token")
    void shouldRejectMalformedToken() {
        assertThatThrownBy(() -> jwtService.extractUserId("not.a.jwt"))
            .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("isValid returns true for a good token, false for a bad one")
    void shouldValidateToken() {
        String good = jwtService.generateToken(42L, "tonko@smartspender.local");

        assertThat(jwtService.isValid(good, "tonko@smartspender.local")).isTrue();
        assertThat(jwtService.isValid(good, "someone-else@x.com")).isFalse();
        assertThat(jwtService.isValid("garbage", "tonko@smartspender.local")).isFalse();
    }
}