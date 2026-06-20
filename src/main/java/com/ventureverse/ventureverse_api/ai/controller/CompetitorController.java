package com.ventureverse.ventureverse_api.ai.controller;

import com.ventureverse.ventureverse_api.ai.dto.CompetitorValidationRequest;
import com.ventureverse.ventureverse_api.ai.dto.CompetitorValidationResponse;
import com.ventureverse.ventureverse_api.ai.service.CompetitorAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/competitor")
@RequiredArgsConstructor
public class CompetitorController {

    private final CompetitorAnalysisService
            competitorAnalysisService;

    @PostMapping("/analyze")
    public CompetitorValidationResponse analyze(
            @RequestBody
            CompetitorValidationRequest request) {

        return competitorAnalysisService
                .analyze(request);
    }
}