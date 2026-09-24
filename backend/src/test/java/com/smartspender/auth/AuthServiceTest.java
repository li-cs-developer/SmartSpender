package com.smartspender.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smartspender.user.User;
import com.smartspender.user.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordHasher passwordHasher;

    @InjectMocks private AuthService authService;

    // =====================================================================
    // register
    // =====================================================================

    @Nested
    @DisplayName("register")
    class Register {

        @Test
        @DisplayName("creates a user with hashed password and returns a token")
        void shouldRegisterNewUser() {
            when(userRepository.existsByEmail("tonko@smartspender.local")).thenReturn(false);
            when(passwordHasher.hash("hunter2hunter2")).thenReturn("$2a$10$hashedvalue");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(jwtService.generateToken(any(), any())).thenReturn("eyJ.token");

            AuthResult result = authService.register(
                "tonko@smartspender.local", "hunter2hunter2", "Tonko"
            );

            assertThat(result.token()).isEqualTo("eyJ.token");
            assertThat(result.email()).isEqualTo("tonko@smartspender.local");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("throws when the email is already taken")
        void shouldRejectDuplicateEmail() {
            when(userRepository.existsByEmail("tonko@smartspender.local")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(
                "tonko@smartspender.local", "hunter2hunter2", "Tonko"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("rejects passwords shorter than 8 characters")
        void shouldRejectShortPassword() {
            assertThatThrownBy(() -> authService.register(
                "new@smartspender.local", "short", "New User"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("normalizes email to lowercase before checking uniqueness")
        void shouldNormalizeEmail() {
            when(userRepository.existsByEmail("tonko@smartspender.local")).thenReturn(false);
            when(passwordHasher.hash(any())).thenReturn("$2a$10$hashed");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> {
                User u = inv.getArgument(0);
                assertThat(u.getEmail()).isEqualTo("tonko@smartspender.local");
                return u;
            });
            when(jwtService.generateToken(any(), any())).thenReturn("token");

            authService.register("Tonko@SmartSpender.Local", "hunter2hunter2", "Tonko");
        }
    }

    // =====================================================================
    // login
    // =====================================================================

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("returns a token when credentials are valid")
        void shouldLoginWithValidCredentials() {
            User user = new User("tonko@smartspender.local", "$2a$10$hashed", "Tonko");
            when(userRepository.findByEmail("tonko@smartspender.local"))
                .thenReturn(Optional.of(user));
            when(passwordHasher.matches("hunter2hunter2", "$2a$10$hashed")).thenReturn(true);
            when(jwtService.generateToken(any(), any())).thenReturn("eyJ.token");

            AuthResult result = authService.login("tonko@smartspender.local", "hunter2hunter2");

            assertThat(result.token()).isEqualTo("eyJ.token");
        }

        @Test
        @DisplayName("throws when the email is unknown")
        void shouldRejectUnknownEmail() {
            when(userRepository.findByEmail("nobody@x.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login("nobody@x.com", "hunter2hunter2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid");
        }

        @Test
        @DisplayName("throws when the password is wrong")
        void shouldRejectWrongPassword() {
            User user = new User("tonko@smartspender.local", "$2a$10$hashed", "Tonko");
            when(userRepository.findByEmail("tonko@smartspender.local"))
                .thenReturn(Optional.of(user));
            when(passwordHasher.matches("wrongpassword", "$2a$10$hashed")).thenReturn(false);

            assertThatThrownBy(() -> authService.login("tonko@smartspender.local", "wrongpassword"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid");
        }
    }
}