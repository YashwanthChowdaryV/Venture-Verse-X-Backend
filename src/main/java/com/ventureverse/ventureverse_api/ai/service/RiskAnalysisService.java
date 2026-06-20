package com.ventureverse.ventureverse_api.ai.service;

import com.ventureverse.ventureverse_api.ai.dto.RiskValidationRequest;
import com.ventureverse.ventureverse_api.ai.dto.RiskValidationResponse;

public interface RiskAnalysisService {

    RiskValidationResponse analyze(
            RiskValidationRequest request);
}