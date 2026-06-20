package com.ventureverse.ventureverse_api.ai.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class CustomerValidationResponse {

    private Integer score;

    private String verdict;

    private String primaryPersona;

    private String secondaryPersona;

    private String painSeverity;

    private String painFrequency;

    private String painUrgency;

    private String adoptionLikelihood;

    private String retentionPotential;

    private String productMarketFit;

    private String customerJourney;

    private List<String> customerObjections;

    private List<String> customerChannels;

    private String marketDemandValidation;

    private String analysis;
}