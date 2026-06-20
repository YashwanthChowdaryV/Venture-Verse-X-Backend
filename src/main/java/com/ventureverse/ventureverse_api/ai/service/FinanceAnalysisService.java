package com.ventureverse.ventureverse_api.ai.service;

import com.ventureverse.ventureverse_api.ai.dto.FinanceValidationRequest;
import com.ventureverse.ventureverse_api.ai.dto.FinanceValidationResponse;

public interface FinanceAnalysisService {

    FinanceValidationResponse analyze(
            FinanceValidationRequest request);
}