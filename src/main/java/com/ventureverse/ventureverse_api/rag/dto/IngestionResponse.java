package com.ventureverse.ventureverse_api.rag.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionResponse {

    private Integer filesProcessed;
    private Integer chunksCreated;
    private Integer documentsStored;
    private String message;
    private List<String> documentIds;
    private Map<String, Integer> categoryDistribution;
}