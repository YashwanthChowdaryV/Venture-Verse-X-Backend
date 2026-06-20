package com.ventureverse.ventureverse_api.ai.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartupExecutiveSummaryResponse {

    private Integer startupReadinessScore;

    private String investmentRecommendation;

    private String fundraisingRecommendation;

    private List<String> topStrengths;

    private List<String> topWeaknesses;

    private List<String> immediateActions;

    private List<String> keyRisks;

    private String executiveSummary;

    private String finalRecommendation;
}