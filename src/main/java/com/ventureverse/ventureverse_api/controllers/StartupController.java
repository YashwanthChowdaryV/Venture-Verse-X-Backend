package com.ventureverse.ventureverse_api.controllers;

import com.ventureverse.ventureverse_api.dto.request.StartupCreateRequest;
import com.ventureverse.ventureverse_api.dto.response.StartupResponse;
import com.ventureverse.ventureverse_api.services.StartupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/startups")
@RequiredArgsConstructor
public class StartupController {

    private final StartupService startupService;

    @PostMapping
    public StartupResponse createStartup(
            @Valid
            @RequestBody StartupCreateRequest request) {

        return startupService.createStartup(request);
    }

    @GetMapping
    public List<StartupResponse> getMyStartups() {

        return startupService.getMyStartups();
    }

    @GetMapping("/{id}")
    public StartupResponse getStartupById(
            @PathVariable Long id) {

        return startupService.getStartupById(id);
    }
}