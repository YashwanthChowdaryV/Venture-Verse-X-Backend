package com.ventureverse.ventureverse_api.ai.controller;

import com.ventureverse.ventureverse_api.ai.dto.RiskValidationRequest;
import com.ventureverse.ventureverse_api.ai.dto.RiskValidationResponse;
import com.ventureverse.ventureverse_api.ai.service.RiskAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskAnalysisService
            riskAnalysisService;

    @PostMapping("/analyze")
    public RiskValidationResponse analyze(
            @RequestBody
            RiskValidationRequest request) {

        return riskAnalysisService
                .analyze(request);
    }
}