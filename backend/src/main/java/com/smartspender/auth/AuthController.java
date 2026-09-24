package com.smartspender.auth;

import com.smartspender.auth.dto.AuthResponse;
import com.smartspender.auth.dto.LoginRequest;
import com.smartspender.auth.dto.RegisterRequest;
import com.smartspender.common.dto.ApiResponse;
import com.smartspender.common.exception.ResourceNotFoundException;
import com.smartspender.config.CurrentUserId;
import com.smartspender.user.User;
import com.smartspender.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResult result = authService.register(
            request.email(), request.password(), request.displayName()
        );
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(AuthResponse.from(result)));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request.email(), request.password());
        return ApiResponse.success(AuthResponse.from(result));
    }

    @GetMapping("/me")
    public ApiResponse<AuthResponse> me(@CurrentUserId Long userId) {
        // We don't need to hit the DB for these three fields, but we will,
        // to guarantee we return the current state of the user (not a stale token).
        User user = userRepository.findById(userId)
            .orElseThrow(() -> ResourceNotFoundException.forId("User", userId));
        return ApiResponse.success(new AuthResponse(
            null, user.getId(), user.getEmail(), user.getDisplayName()
        ));
    }
}