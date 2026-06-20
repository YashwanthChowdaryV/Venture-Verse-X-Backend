package com.ventureverse.ventureverse_api.ai.controller;

import com.ventureverse.ventureverse_api.ai.dto.AnalysisHistoryResponse;
import com.ventureverse.ventureverse_api.ai.dto.IdeaValidationRequest;
import com.ventureverse.ventureverse_api.ai.dto.IdeaValidationResponse;
import com.ventureverse.ventureverse_api.ai.service.IdeaValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIController {

    private final IdeaValidationService
            ideaValidationService;

    @PostMapping("/validate")
    public IdeaValidationResponse validateIdea(
            @RequestBody
            IdeaValidationRequest request) {

        return ideaValidationService
                .validateIdea(request);
    }

    @GetMapping("/history/{startupId}")
    public List<AnalysisHistoryResponse>
    getHistory(
            @PathVariable Long startupId) {

        return ideaValidationService
                .getHistory(startupId);
    }
}