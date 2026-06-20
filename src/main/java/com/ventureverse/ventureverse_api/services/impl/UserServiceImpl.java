package com.ventureverse.ventureverse_api.services.impl;

import com.ventureverse.ventureverse_api.security.SecurityUtils;
import com.ventureverse.ventureverse_api.dto.request.LoginRequest;
import com.ventureverse.ventureverse_api.dto.request.UserCreateRequest;
import com.ventureverse.ventureverse_api.dto.response.AuthResponse;
import com.ventureverse.ventureverse_api.dto.response.UserResponse;
import com.ventureverse.ventureverse_api.entities.User;
import com.ventureverse.ventureverse_api.repositories.UserRepository;
import com.ventureverse.ventureverse_api.security.JwtService;
import com.ventureverse.ventureverse_api.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;

        @Override
        public UserResponse createUser(UserCreateRequest request) {

                // Check email uniqueness
                if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
                        throw new RuntimeException("Email already exists. Please use a different email.");
                }

                // Check username uniqueness
                if (userRepository.existsByUsername(request.getUsername().toLowerCase().trim())) {
                        throw new RuntimeException("Username already taken. Please choose a different username.");
                }

                // Create user with formatted data
                User user = User.builder()
                                .fullName(request.getFullName().trim())
                                .username(request.getUsername().toLowerCase().trim())
                                .email(request.getEmail().toLowerCase().trim())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .createdAt(LocalDateTime.now())
                                .enabled(true)
                                .emailVerified(false)
                                .build();

                User savedUser = userRepository.save(user);

                return UserResponse.builder()
                                .id(savedUser.getId())
                                .fullName(savedUser.getFullName())
                                .username(savedUser.getUsername())
                                .email(savedUser.getEmail())
                                .emailVerified(savedUser.getEmailVerified())
                                .createdAt(savedUser.getCreatedAt())
                                .build();
        }

        @Override
        public UserResponse getCurrentUser() {

                String email = SecurityUtils.getCurrentUserEmail();

                User user = userRepository.findByEmail(email)
                                .orElseThrow(() -> new RuntimeException("User not found"));

                return UserResponse.builder()
                                .id(user.getId())
                                .fullName(user.getFullName())
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .emailVerified(user.getEmailVerified())
                                .createdAt(user.getCreatedAt())
                                .build();
        }

        @Override
        public AuthResponse login(LoginRequest request) {

                // Find user by email OR username - SPECIFIC ERROR
                User user = userRepository.findByEmailOrUsername(request.getLogin().toLowerCase().trim())
                                .orElseThrow(() -> new RuntimeException(
                                                "No account found with this email or username. Please create an account first."));

                // Check if account is enabled
                if (!user.getEnabled()) {
                        throw new RuntimeException("Account is disabled. Please contact support.");
                }

                // Verify password - SPECIFIC ERROR
                boolean matches = passwordEncoder.matches(
                                request.getPassword(),
                                user.getPassword());

                if (!matches) {
                        throw new RuntimeException("Incorrect password. Please enter the correct password.");
                }

                // Update last login timestamp
                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user);

                // Generate JWT token with username
                String token = jwtService.generateToken(user.getUsername());

                return AuthResponse.builder()
                                .token(token)
                                .id(user.getId())
                                .fullName(user.getFullName())
                                .username(user.getUsername())
                                .email(user.getEmail())
                                .emailVerified(user.getEmailVerified())
                                .build();
        }

        // Additional utility methods

        public boolean usernameExists(String username) {
                return userRepository.existsByUsername(username.toLowerCase().trim());
        }

        public boolean emailExists(String email) {
                return userRepository.existsByEmail(email.toLowerCase().trim());
        }

        public User findByUsername(String username) {
                return userRepository.findByUsername(username.toLowerCase().trim())
                                .orElseThrow(() -> new RuntimeException("User not found with username: " + username));
        }

        public User findByEmail(String email) {
                return userRepository.findByEmail(email.toLowerCase().trim())
                                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        }
}