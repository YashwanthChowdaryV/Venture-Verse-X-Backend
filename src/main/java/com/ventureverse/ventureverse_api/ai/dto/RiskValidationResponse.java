package com.ventureverse.ventureverse_api.ai.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskValidationResponse {

    private Integer score;

    private String verdict;

    private String marketRisk;

    private String executionRisk;

    private String financialRisk;

    private String regulatoryRisk;

    private String operationalRisk;

    private String technologyRisk;

    private String scalabilityRisk;

    private String adoptionRisk;

    private String founderRisk;

    private List<String> topRisks;

    private List<String> mitigationStrategies;

    private String analysis;
}