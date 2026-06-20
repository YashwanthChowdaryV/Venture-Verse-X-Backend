package com.ventureverse.ventureverse_api.ai.service;

import com.ventureverse.ventureverse_api.ai.dto.StartupExecutiveSummaryResponse;

public interface ExecutiveSummaryService {

    StartupExecutiveSummaryResponse generateSummary(
            String investorAnalysis,
            String competitorAnalysis,
            String financeAnalysis,
            String customerAnalysis,
            String riskAnalysis,
            String productStrategyAnalysis
    );
}