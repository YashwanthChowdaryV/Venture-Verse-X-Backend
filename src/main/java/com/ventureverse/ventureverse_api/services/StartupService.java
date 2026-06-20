package com.ventureverse.ventureverse_api.services;

import com.ventureverse.ventureverse_api.dto.request.StartupCreateRequest;
import com.ventureverse.ventureverse_api.dto.response.StartupResponse;

import java.util.List;

public interface StartupService {

    StartupResponse createStartup(StartupCreateRequest request);

    List<StartupResponse> getMyStartups();

    StartupResponse getStartupById(Long id);
}