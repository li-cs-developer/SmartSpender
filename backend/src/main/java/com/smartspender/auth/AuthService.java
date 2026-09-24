package com.smartspender.auth;

import com.smartspender.user.User;
import com.smartspender.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtService jwtService;

    @Transactional
    public AuthResult register(String email, String rawPassword, String displayName) {
        String normalizedEmail = normalizeEmail(email);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email is already registered: " + normalizedEmail);
        }
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long"
            );
        }

        String hashed = passwordHasher.hash(rawPassword);
        User user = userRepository.save(new User(normalizedEmail, hashed, displayName));

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResult(token, user.getId(), user.getEmail(), user.getDisplayName());
    }

    @Transactional(readOnly = true)
    public AuthResult login(String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);

        User user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordHasher.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthResult(token, user.getId(), user.getEmail(), user.getDisplayName());
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be blank");
        }
        return email.trim().toLowerCase();
    }
}