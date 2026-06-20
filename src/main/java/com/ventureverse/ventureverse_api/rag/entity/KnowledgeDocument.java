package com.ventureverse.ventureverse_api.rag.entity;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument {

    private String id;
    private String title;
    private String content;

    // Metadata for better retrieval
    private String category; // fundraising, metrics, strategy, legal, growth
    private String topic; // series_a, tam_sam_som, gtm, product_market_fit
    private List<String> keywords; // ["series a", "funding", "VC", "valuation"]
    private String difficulty; // beginner, intermediate, advanced
    private String persona; // founder, investor, analyst
    private String sourceType; // expert_article, research, case_study, guide
    private String author;
    private LocalDateTime publicationDate;
    private Double freshnessScore; // 0.0 - 1.0
    private Double authorityScore; // 0.0 - 1.0
    private String version;
    private Map<String, Object> customMetadata;
}