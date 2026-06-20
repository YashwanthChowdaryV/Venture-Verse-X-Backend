package com.ventureverse.ventureverse_api.ai.orchestrator.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartupReportHistoryResponse {

    private Long id;

    private Integer overallScore;

    private String finalVerdict;

    private LocalDateTime createdAt;
}