package com.ventureverse.ventureverse_api.controllers;

import com.ventureverse.ventureverse_api.dto.request.LoginRequest;
import com.ventureverse.ventureverse_api.dto.request.UserCreateRequest;
import com.ventureverse.ventureverse_api.dto.response.AuthResponse;
import com.ventureverse.ventureverse_api.dto.response.UserResponse;
import com.ventureverse.ventureverse_api.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins:http://localhost:5173}")
public class AuthController {

    private final UserService userService;

    /**
     * Register new user
     * POST /api/v1/auth/register
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody UserCreateRequest request) {

        // Create user (we don't need the response here since we're auto-logging in)
        userService.createUser(request);

        // Auto-login after registration using username
        LoginRequest loginRequest = LoginRequest.builder()
                .login(request.getUsername())
                .password(request.getPassword())
                .build();

        AuthResponse authResponse = userService.login(loginRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authResponse);
    }

    /**
     * Login with email OR username
     * POST /api/v1/auth/login
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse authResponse = userService.login(request);

        return ResponseEntity.ok(authResponse);
    }

    /**
     * Get current authenticated user
     * GET /api/v1/auth/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        UserResponse userResponse = userService.getCurrentUser();
        return ResponseEntity.ok(userResponse);
    }

    /**
     * Check username availability
     * GET /api/v1/auth/check-username?username=yash
     */
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Boolean>> checkUsername(
            @RequestParam String username) {

        boolean exists = userService.usernameExists(username);

        return ResponseEntity.ok(Map.of("available", !exists));
    }

    /**
     * Check email availability
     * GET /api/v1/auth/check-email?email=yash@example.com
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Boolean>> checkEmail(
            @RequestParam String email) {

        boolean exists = userService.emailExists(email);

        return ResponseEntity.ok(Map.of("available", !exists));
    }
}