package com.ventureverse.ventureverse_api.controllers;

import com.ventureverse.ventureverse_api.dto.request.UserCreateRequest;
import com.ventureverse.ventureverse_api.dto.response.UserResponse;
import com.ventureverse.ventureverse_api.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public UserResponse createUser(
            @Valid
            @RequestBody UserCreateRequest request) {

        return userService.createUser(request);
    }

    @GetMapping("/me")
    public UserResponse currentUser() {

        return userService.getCurrentUser();
    }
}