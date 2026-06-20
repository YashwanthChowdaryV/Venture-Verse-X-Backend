package com.ventureverse.ventureverse_api.ai.service;

import com.ventureverse.ventureverse_api.ai.dto.CustomerValidationRequest;
import com.ventureverse.ventureverse_api.ai.dto.CustomerValidationResponse;

public interface CustomerAnalysisService {

    CustomerValidationResponse analyze(
            CustomerValidationRequest request);
}