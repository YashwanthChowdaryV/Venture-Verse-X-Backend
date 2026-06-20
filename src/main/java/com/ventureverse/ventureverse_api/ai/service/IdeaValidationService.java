package com.ventureverse.ventureverse_api.ai.service;

import com.ventureverse.ventureverse_api.ai.dto.AnalysisHistoryResponse;
import com.ventureverse.ventureverse_api.ai.dto.IdeaValidationRequest;
import com.ventureverse.ventureverse_api.ai.dto.IdeaValidationResponse;

import java.util.List;

public interface IdeaValidationService {

    IdeaValidationResponse validateIdea(
            IdeaValidationRequest request);

    List<AnalysisHistoryResponse> getHistory(
            Long startupId);
}