package com.ventureverse.ventureverse_api.services;

import com.ventureverse.ventureverse_api.dto.request.LoginRequest;
import com.ventureverse.ventureverse_api.dto.request.UserCreateRequest;
import com.ventureverse.ventureverse_api.dto.response.AuthResponse;
import com.ventureverse.ventureverse_api.dto.response.UserResponse;

public interface UserService {

    UserResponse createUser(UserCreateRequest request);

    UserResponse getCurrentUser();

    AuthResponse login(LoginRequest request);

    // NEW: Username and email availability checks
    boolean usernameExists(String username);

    boolean emailExists(String email);
}