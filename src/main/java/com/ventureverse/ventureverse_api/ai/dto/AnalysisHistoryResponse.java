package com.ventureverse.ventureverse_api.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisHistoryResponse {

    private Long id;

    private Integer score;

    private String response;

    private LocalDateTime createdAt;
}