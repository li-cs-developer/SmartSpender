package com.smartspender.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Generates and validates JWTs for authenticated users.
 *
 * Kept stateless and side-effect free — it doesn't touch the database.
 * The token itself carries the user's identity.
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMillis;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMillis) {

        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                "JWT secret must be at least 32 bytes for HS256"
            );
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    /** Creates a signed JWT containing the user's id (subject) and email. */
    public String generateToken(Long userId, String email) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(signingKey)
            .compact();
    }

    /** Extracts the user id from the token's subject. Throws if invalid or expired. */
    public Long extractUserId(String token) {
        String subject = parseClaims(token).getSubject();
        return Long.valueOf(subject);
    }

    /** Extracts the email claim. */
    public String extractEmail(String token) {
        return parseClaims(token).get("email", String.class);
    }

    /** Verifies signature + expiry AND that the email claim matches. */
    public boolean isValid(String token, String expectedEmail) {
        try {
            Claims claims = parseClaims(token);
            return expectedEmail.equals(claims.get("email", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ---- internals ----

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}