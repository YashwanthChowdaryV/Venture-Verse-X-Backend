package com.ventureverse.ventureverse_api.ai.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStrategyResponse {

    private Integer score;

    private String verdict;

    private List<String> mvpFeatures;

    private List<String> mustHaveFeatures;

    private List<String> shouldHaveFeatures;

    private List<String> couldHaveFeatures;

    private List<String> phase1Roadmap;

    private List<String> phase2Roadmap;

    private List<String> phase3Roadmap;

    private List<String> gtmStrategy;

    private List<String> validationPlan;

    private List<String> growthStrategy;

    private List<String> kpis;

    private List<String> next90Days;

    private String analysis;
}