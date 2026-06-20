package com.ventureverse.ventureverse_api.ai.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitorValidationResponse {

    private Integer score;

    private String verdict;

    private String marketSaturation;

    private String competitivePosition;

    private List<String> directCompetitors;

    private List<String> indirectCompetitors;

    private List<String> moats;

    private List<String> barriersToEntry;

    private List<String> competitiveGaps;

    private List<String> marketGaps;

    private List<String> strengths;

    private List<String> threats;

    private String analysis;
}