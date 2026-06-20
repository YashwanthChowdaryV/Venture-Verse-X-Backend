package com.ventureverse.ventureverse_api.ai.service;

import com.ventureverse.ventureverse_api.ai.dto.ProductStrategyRequest;
import com.ventureverse.ventureverse_api.ai.dto.ProductStrategyResponse;

public interface ProductStrategyService {

    ProductStrategyResponse analyze(
            ProductStrategyRequest request);
}