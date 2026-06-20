package com.ventureverse.ventureverse_api.rag.entity;

import lombok.*;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeGraphNode {
    private String id;
    private String label;
    private String type; // CONCEPT, METRIC, FRAMEWORK, PERSON, COMPANY
    private String domain; // fundraising, metrics, strategy, product, growth
    private String description;
    private Map<String, Object> properties;
}