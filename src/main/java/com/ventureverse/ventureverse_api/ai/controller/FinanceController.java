package com.ventureverse.ventureverse_api.ai.controller;

import com.ventureverse.ventureverse_api.ai.dto.FinanceValidationRequest;
import com.ventureverse.ventureverse_api.ai.dto.FinanceValidationResponse;
import com.ventureverse.ventureverse_api.ai.service.FinanceAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceAnalysisService
            financeAnalysisService;

    @PostMapping("/analyze")
    public FinanceValidationResponse analyze(
            @RequestBody
            FinanceValidationRequest request) {

        return financeAnalysisService
                .analyze(request);
    }
}