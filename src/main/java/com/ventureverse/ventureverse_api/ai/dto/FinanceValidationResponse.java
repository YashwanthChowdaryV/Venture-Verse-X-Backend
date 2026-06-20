package com.ventureverse.ventureverse_api.ai.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceValidationResponse {

    private Integer score;

    private String verdict;

    private List<String> revenueStreams;

    private String pricingStrategy;

    private String unitEconomics;

    private String cac;

    private String ltv;

    private String ltvCacRatio;

    private String burnRate;

    private String runway;

    private String year1Revenue;

    private String year2Revenue;

    private String year3Revenue;

    private String fundraisingNeed;

    private String requiredCapital;

    private String useOfFunds;

    private String profitabilityTimeline;

    private String financialSustainability;

    private String analysis;
}