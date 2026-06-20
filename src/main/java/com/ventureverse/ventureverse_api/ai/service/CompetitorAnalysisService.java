package com.ventureverse.ventureverse_api.ai.service;

import com.ventureverse.ventureverse_api.ai.dto.CompetitorValidationRequest;
import com.ventureverse.ventureverse_api.ai.dto.CompetitorValidationResponse;

public interface CompetitorAnalysisService {

    CompetitorValidationResponse analyze(
            CompetitorValidationRequest request);
}