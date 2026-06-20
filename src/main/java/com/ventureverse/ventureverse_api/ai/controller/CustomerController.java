package com.ventureverse.ventureverse_api.ai.controller;

import com.ventureverse.ventureverse_api.ai.dto.CustomerValidationRequest;
import com.ventureverse.ventureverse_api.ai.dto.CustomerValidationResponse;
import com.ventureverse.ventureverse_api.ai.service.CustomerAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerAnalysisService
            customerAnalysisService;

    @PostMapping("/analyze")
    public CustomerValidationResponse analyze(
            @RequestBody
            CustomerValidationRequest request) {

        return customerAnalysisService
                .analyze(request);
    }
}