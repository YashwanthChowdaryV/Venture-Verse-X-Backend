package com.ventureverse.ventureverse_api.ai.orchestrator.dto;
import com.ventureverse.ventureverse_api.ai.dto.StartupExecutiveSummaryResponse;

import com.ventureverse.ventureverse_api.ai.dto.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartupFullAnalysisResponse {

    private Integer overallScore;

    private Integer investmentScore;

    private Integer competitionScore;

    private Integer financialScore;

    private Integer customerScore;

    private Integer riskScore;

    private Integer productStrategyScore;

    private String finalVerdict;

    /*
     * Legacy Summary Fields
     */
    private String investorAnalysis;

    private String competitorAnalysis;

    private String financeAnalysis;

    private String customerAnalysis;

    private String riskAnalysis;

    private String productStrategyAnalysis;

    /*
     * Detailed Agent Outputs
     */
    private IdeaValidationResponse investorDetails;

    private CompetitorValidationResponse competitorDetails;

    private FinanceValidationResponse financeDetails;

    private CustomerValidationResponse customerDetails;

    private RiskValidationResponse riskDetails;

    private ProductStrategyResponse productStrategyDetails;
    private StartupExecutiveSummaryResponse executiveSummary;
}