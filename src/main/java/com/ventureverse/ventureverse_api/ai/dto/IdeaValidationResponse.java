package com.ventureverse.ventureverse_api.ai.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdeaValidationResponse {

    private Integer score;

    private String verdict;

    private String investmentAttractiveness;

    private String marketSize;

    private String tam;

    private String sam;

    private String som;

    private String fundability;

    private String vcAppeal;

    private String startupStageFit;

    private List<String> strengths;

    private List<String> weaknesses;

    private List<String> opportunities;

    private List<String> threats;

    private String longTermOpportunity;

    private String analysis;
}