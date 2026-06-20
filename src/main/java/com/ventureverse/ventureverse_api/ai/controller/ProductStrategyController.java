package com.ventureverse.ventureverse_api.ai.controller;

import com.ventureverse.ventureverse_api.ai.dto.ProductStrategyRequest;
import com.ventureverse.ventureverse_api.ai.dto.ProductStrategyResponse;
import com.ventureverse.ventureverse_api.ai.service.ProductStrategyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/product-strategy")
@RequiredArgsConstructor
public class ProductStrategyController {

    private final ProductStrategyService
            productStrategyService;

    @PostMapping("/analyze")
    public ProductStrategyResponse analyze(
            @RequestBody
            ProductStrategyRequest request) {

        return productStrategyService
                .analyze(request);
    }
}