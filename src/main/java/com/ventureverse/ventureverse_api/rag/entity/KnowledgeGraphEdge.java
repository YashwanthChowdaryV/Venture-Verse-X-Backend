package com.ventureverse.ventureverse_api.rag.entity;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeGraphEdge {
    private String id;
    private String sourceNodeId;
    private String targetNodeId;
    private String relationship; // RELATES_TO, PREREQUISITE_OF, PART_OF, EXAMPLE_OF, MEASURES
    private double weight;
}