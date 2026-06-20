package com.ventureverse.ventureverse_api.rag.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeUploadRequest {

    private String title;
    private String content;

    // Metadata
    private String category;
    private String topic;
    private List<String> keywords;
    private String difficulty;
    private String persona;
    private String sourceType;
    private String author;
    private Double freshnessScore;
    private Double authorityScore;
    private Map<String, Object> customMetadata;
}