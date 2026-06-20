package com.ventureverse.ventureverse_api.rag.knowledge.graph;

import com.ventureverse.ventureverse_api.rag.entity.KnowledgeGraphEdge;
import com.ventureverse.ventureverse_api.rag.entity.KnowledgeGraphNode;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeGraphBuilder {

    // Pre-built startup knowledge graph
    private static final Map<String, List<String[]>> GRAPH_RELATIONSHIPS = new LinkedHashMap<>();

    static {
        // TAM → SAM → SOM hierarchy
        GRAPH_RELATIONSHIPS.put("TAM", List.of(
                new String[] { "SAM", "PART_OF" },
                new String[] { "Market Sizing", "BELONGS_TO" },
                new String[] { "VC Evaluation", "USED_IN" }));
        GRAPH_RELATIONSHIPS.put("SAM", List.of(
                new String[] { "TAM", "DERIVED_FROM" },
                new String[] { "SOM", "CONTAINS" },
                new String[] { "Business Model", "CONSTRAINED_BY" }));
        GRAPH_RELATIONSHIPS.put("SOM", List.of(
                new String[] { "SAM", "DERIVED_FROM" },
                new String[] { "Growth Rate", "MEASURED_BY" },
                new String[] { "Market Share", "EQUIVALENT_TO" }));

        // Product-Market Fit relationships
        GRAPH_RELATIONSHIPS.put("Product-Market Fit", List.of(
                new String[] { "Customer Discovery", "PREREQUISITE_OF" },
                new String[] { "Retention", "MEASURED_BY" },
                new String[] { "Sean Ellis Test", "MEASURED_BY" },
                new String[] { "Series A", "PREREQUISITE_FOR" }));

        // Funding relationships
        GRAPH_RELATIONSHIPS.put("Series A", List.of(
                new String[] { "Product-Market Fit", "REQUIRES" },
                new String[] { "ARR", "MEASURED_BY" },
                new String[] { "Valuation", "DETERMINES" },
                new String[] { "Term Sheet", "RESULTS_IN" }));
        GRAPH_RELATIONSHIPS.put("Seed Funding", List.of(
                new String[] { "MVP", "FUNDS" },
                new String[] { "Angel Investor", "SOURCED_FROM" },
                new String[] { "SAFE", "INSTRUMENT" }));

        // Metrics relationships
        GRAPH_RELATIONSHIPS.put("CAC", List.of(
                new String[] { "LTV", "COMPARED_WITH" },
                new String[] { "Marketing Spend", "CALCULATED_FROM" },
                new String[] { "Payback Period", "DETERMINES" }));
        GRAPH_RELATIONSHIPS.put("LTV", List.of(
                new String[] { "CAC", "COMPARED_WITH" },
                new String[] { "Churn Rate", "AFFECTED_BY" },
                new String[] { "Expansion Revenue", "INCREASED_BY" }));
        GRAPH_RELATIONSHIPS.put("Churn Rate", List.of(
                new String[] { "Retention", "INVERSE_OF" },
                new String[] { "Customer Success", "REDUCED_BY" },
                new String[] { "Product-Market Fit", "INDICATES" }));
        GRAPH_RELATIONSHIPS.put("MRR", List.of(
                new String[] { "ARR", "ANNUALIZED_TO" },
                new String[] { "SaaS Metrics", "BELONGS_TO" },
                new String[] { "Growth Rate", "CALCULATES" }));

        // Strategy relationships
        GRAPH_RELATIONSHIPS.put("GTM Strategy", List.of(
                new String[] { "Product-Led Growth", "INCLUDES" },
                new String[] { "Sales-Led Growth", "INCLUDES" },
                new String[] { "Target Customer", "DEFINES" },
                new String[] { "Distribution", "FOCUSES_ON" }));
        GRAPH_RELATIONSHIPS.put("Network Effects", List.of(
                new String[] { "Competitive Moat", "CREATES" },
                new String[] { "Marketplace", "POWERS" },
                new String[] { "Viral Growth", "ENABLES" }));

        // Risk relationships
        GRAPH_RELATIONSHIPS.put("Startup Failure", List.of(
                new String[] { "No Market Need", "CAUSED_BY" },
                new String[] { "Ran Out of Cash", "CAUSED_BY" },
                new String[] { "Wrong Team", "CAUSED_BY" },
                new String[] { "Competition", "CAUSED_BY" }));
    }

    private final Map<String, KnowledgeGraphNode> nodes = new LinkedHashMap<>();
    private final List<KnowledgeGraphEdge> edges = new ArrayList<>();

    public KnowledgeGraphBuilder() {
        buildGraph();
    }

    private void buildGraph() {
        for (Map.Entry<String, List<String[]>> entry : GRAPH_RELATIONSHIPS.entrySet()) {
            String sourceLabel = entry.getKey();

            // Create source node if not exists
            nodes.computeIfAbsent(sourceLabel, label -> KnowledgeGraphNode.builder()
                    .id(generateId(label))
                    .label(label)
                    .type(detectType(label))
                    .domain(detectDomain(label))
                    .build());

            for (String[] relationship : entry.getValue()) {
                String targetLabel = relationship[0];
                String relationType = relationship[1];

                // Create target node if not exists
                nodes.computeIfAbsent(targetLabel, label -> KnowledgeGraphNode.builder()
                        .id(generateId(label))
                        .label(label)
                        .type(detectType(label))
                        .domain(detectDomain(label))
                        .build());

                edges.add(KnowledgeGraphEdge.builder()
                        .id(UUID.randomUUID().toString())
                        .sourceNodeId(nodes.get(sourceLabel).getId())
                        .targetNodeId(nodes.get(targetLabel).getId())
                        .relationship(relationType)
                        .weight(1.0)
                        .build());
            }
        }
    }

    /**
     * Get related concepts for a query term
     */
    public List<KnowledgeGraphNode> getRelatedConcepts(String term, int depth) {
        Set<String> visited = new HashSet<>();
        List<KnowledgeGraphNode> related = new ArrayList<>();

        KnowledgeGraphNode startNode = findNode(term);
        if (startNode != null) {
            traverseGraph(startNode.getId(), depth, visited, related);
        }

        return related;
    }

    /**
     * Get relationship path between two concepts
     */
    public List<KnowledgeGraphEdge> getPath(String from, String to) {
        KnowledgeGraphNode startNode = findNode(from);
        KnowledgeGraphNode endNode = findNode(to);

        if (startNode == null || endNode == null)
            return Collections.emptyList();

        // Simple BFS to find path
        return findPath(startNode.getId(), endNode.getId());
    }

    /**
     * Get all concepts in a domain
     */
    public List<KnowledgeGraphNode> getDomainConcepts(String domain) {
        return nodes.values().stream()
                .filter(n -> domain.equalsIgnoreCase(n.getDomain()))
                .collect(Collectors.toList());
    }

    /**
     * Expand query with related terms for better retrieval
     */
    public List<String> expandQuery(String query) {
        List<String> expanded = new ArrayList<>();
        expanded.add(query);

        for (String term : query.split("\\s+")) {
            String normalized = term.replaceAll("[^a-zA-Z]", "").toUpperCase();
            KnowledgeGraphNode node = findNodeByLabel(normalized);

            if (node != null) {
                // Add related concepts
                List<KnowledgeGraphNode> related = getRelatedConcepts(normalized, 1);
                expanded.addAll(related.stream()
                        .map(KnowledgeGraphNode::getLabel)
                        .collect(Collectors.toList()));
            }
        }

        return expanded.stream().distinct().collect(Collectors.toList());
    }

    private KnowledgeGraphNode findNode(String term) {
        return nodes.values().stream()
                .filter(n -> n.getLabel().equalsIgnoreCase(term) ||
                        n.getId().equalsIgnoreCase(term))
                .findFirst()
                .orElse(null);
    }

    private KnowledgeGraphNode findNodeByLabel(String label) {
        return nodes.values().stream()
                .filter(n -> n.getLabel().equalsIgnoreCase(label))
                .findFirst()
                .orElse(null);
    }

    private void traverseGraph(String nodeId, int depth, Set<String> visited, List<KnowledgeGraphNode> result) {
        if (depth < 0 || visited.contains(nodeId))
            return;
        visited.add(nodeId);

        nodes.values().stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst()
                .ifPresent(result::add);

        if (depth > 0) {
            edges.stream()
                    .filter(e -> e.getSourceNodeId().equals(nodeId) || e.getTargetNodeId().equals(nodeId))
                    .forEach(e -> {
                        String nextId = e.getSourceNodeId().equals(nodeId) ? e.getTargetNodeId() : e.getSourceNodeId();
                        traverseGraph(nextId, depth - 1, visited, result);
                    });
        }
    }

    private List<KnowledgeGraphEdge> findPath(String from, String to) {
        Queue<List<String>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.add(List.of(from));
        visited.add(from);

        while (!queue.isEmpty()) {
            List<String> path = queue.poll();
            String current = path.get(path.size() - 1);

            if (current.equals(to)) {
                return buildEdgePath(path);
            }

            for (KnowledgeGraphEdge edge : edges) {
                String next = null;
                if (edge.getSourceNodeId().equals(current))
                    next = edge.getTargetNodeId();
                else if (edge.getTargetNodeId().equals(current))
                    next = edge.getSourceNodeId();

                if (next != null && !visited.contains(next)) {
                    visited.add(next);
                    List<String> newPath = new ArrayList<>(path);
                    newPath.add(next);
                    queue.add(newPath);
                }
            }
        }
        return Collections.emptyList();
    }

    private List<KnowledgeGraphEdge> buildEdgePath(List<String> path) {
        List<KnowledgeGraphEdge> edgePath = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            final int idx = i;
            edges.stream()
                    .filter(e -> (e.getSourceNodeId().equals(path.get(idx))
                            && e.getTargetNodeId().equals(path.get(idx + 1))) ||
                            (e.getTargetNodeId().equals(path.get(idx))
                                    && e.getSourceNodeId().equals(path.get(idx + 1))))
                    .findFirst()
                    .ifPresent(edgePath::add);
        }
        return edgePath;
    }

    private String generateId(String label) {
        return label.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }

    private String detectType(String label) {
        if (label.contains("Rate") || label.contains("CAC") || label.contains("LTV") ||
                label.contains("MRR") || label.contains("ARR") || label.contains("KPI")) {
            return "METRIC";
        }
        if (label.contains("Strategy") || label.contains("Framework") || label.contains("Methodology")) {
            return "FRAMEWORK";
        }
        if (label.contains("Funding") || label.contains("Series") || label.contains("Seed")) {
            return "FUNDING_STAGE";
        }
        return "CONCEPT";
    }

    private String detectDomain(String label) {
        if (label.contains("TAM") || label.contains("SAM") || label.contains("SOM") ||
                label.contains("Market") || label.contains("CAC") || label.contains("LTV") ||
                label.contains("MRR") || label.contains("ARR") || label.contains("Churn")) {
            return "metrics";
        }
        if (label.contains("Funding") || label.contains("Series") || label.contains("Seed") ||
                label.contains("VC") || label.contains("Valuation") || label.contains("Term Sheet")) {
            return "fundraising";
        }
        if (label.contains("Product") || label.contains("MVP") || label.contains("Market Fit")) {
            return "product";
        }
        if (label.contains("Strategy") || label.contains("GTM") || label.contains("Growth")) {
            return "strategy";
        }
        return "general";
    }

    public Map<String, KnowledgeGraphNode> getAllNodes() {
        return nodes;
    }

    public List<KnowledgeGraphEdge> getAllEdges() {
        return edges;
    }
}