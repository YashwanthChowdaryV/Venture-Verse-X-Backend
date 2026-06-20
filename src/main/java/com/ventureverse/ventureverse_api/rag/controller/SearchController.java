package com.ventureverse.ventureverse_api.rag.controller;

import com.ventureverse.ventureverse_api.rag.cache.SemanticCache;
import com.ventureverse.ventureverse_api.rag.core.context.ContextCompressor;
import com.ventureverse.ventureverse_api.rag.core.context.ContextCompressor.CompressedContext;
import com.ventureverse.ventureverse_api.rag.generation.AnswerGenerator;
import com.ventureverse.ventureverse_api.rag.generation.AnswerGenerator.GeneratedAnswer;
import com.ventureverse.ventureverse_api.rag.generation.CitationGenerator;
import com.ventureverse.ventureverse_api.rag.generation.CitationGenerator.Citation;
import com.ventureverse.ventureverse_api.rag.generation.CitationGenerator.CitedAnswer;
import com.ventureverse.ventureverse_api.rag.knowledge.graph.KnowledgeGraphBuilder;
import com.ventureverse.ventureverse_api.rag.knowledge.memory.ConversationMemory;
import com.ventureverse.ventureverse_api.rag.observability.MetricsCollector;
import com.ventureverse.ventureverse_api.rag.entity.KnowledgeGraphEdge;
import com.ventureverse.ventureverse_api.rag.entity.KnowledgeGraphNode;
import com.ventureverse.ventureverse_api.rag.retrieval.hybrid.HybridRetriever;
import com.ventureverse.ventureverse_api.rag.retrieval.hybrid.HybridRetriever.HybridResult;
import com.ventureverse.ventureverse_api.rag.retrieval.hybrid.HybridRetriever.ScoredDocument;
import com.ventureverse.ventureverse_api.rag.retrieval.rerank.CrossEncoderReranker;
import com.ventureverse.ventureverse_api.rag.retrieval.rerank.CrossEncoderReranker.RerankedResult;
import com.ventureverse.ventureverse_api.rag.service.RagSearchService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final HybridRetriever hybridRetriever;
    private final CrossEncoderReranker reranker;
    private final CitationGenerator citationGenerator;
    private final AnswerGenerator answerGenerator;
    private final ContextCompressor contextCompressor;
    private final SemanticCache semanticCache;
    private final KnowledgeGraphBuilder knowledgeGraph;
    private final ConversationMemory conversationMemory;
    private final MetricsCollector metrics;
    private final RagSearchService ragSearchService;

    // ==================== REQUEST/RESPONSE DTOS ====================

    @Data
    @Builder
    public static class SearchRequest {
        private String query;
        private int topK;
        private boolean includeCitations;
        private boolean includeReranking;
    }

    @Data
    @Builder
    public static class SearchResponse {
        private String query;
        private List<SearchResultItem> results;
        private SearchMetadata metadata;
        private Boolean fromCache;
    }

    @Data
    @Builder
    public static class SearchResultItem {
        private int rank;
        private String content;
        private String title;
        private double score;
        private List<Citation> citations;
        private String annotatedContent;
    }

    @Data
    @Builder
    public static class SearchMetadata {
        private long retrievalTimeMs;
        private long rerankTimeMs;
        private int totalCandidates;
        private int finalCount;
        private double averageConfidence;
        private String retrievalMethod;
    }

    @Data
    @Builder
    public static class AnswerResponse {
        private String query;
        private String answer;
        private List<Citation> citations;
        private double confidence;
        private String model;
        private long retrievalTimeMs;
        private long generationTimeMs;
        private Double compressionRatio;
        private Integer originalTokens;
        private Integer compressedTokens;
        private Boolean fromCache;
    }

    @Data
    @Builder
    public static class DebugSearchResponse {
        private String query;
        private int totalResults;
        private List<DebugResultItem> results;
        private Map<String, Object> stats;
    }

    @Data
    @Builder
    public static class DebugResultItem {
        private double score;
        private String title;
        private String documentId;
        private String contentPreview;
        private String category;
        private String subcategory;
        private String stage;
    }

    @Data
    @Builder
    public static class SearchStatsResponse {
        private String query;
        private int totalResults;
        private String maxScore;
        private String avgScore;
        private long highQualityResults;
        private int searchLimit;
        private double minRelevanceThreshold;
        private List<String> topTitles;
    }

    // ==================== STANDARD SEARCH ENDPOINTS ====================

    @PostMapping
    public ResponseEntity<SearchResponse> search(@RequestBody SearchRequest request) {
        String query = request.getQuery();
        int topK = request.getTopK() > 0 ? request.getTopK() : 5;
        boolean fromCache = false;

        Optional<HybridResult> cached = semanticCache.getRetrieval(query);
        HybridResult hybridResult;

        if (cached.isPresent()) {
            hybridResult = cached.get();
            fromCache = true;
        } else {
            hybridResult = hybridRetriever.searchWithIntent(query);
            semanticCache.putRetrieval(query, hybridResult);
        }

        long rerankStart = System.currentTimeMillis();
        RerankedResult rerankedResult = reranker.rerank(query, hybridResult.getDocuments());
        long rerankTime = System.currentTimeMillis() - rerankStart;

        List<ScoredDocument> topDocs = rerankedResult.getDocuments().stream()
                .limit(topK).collect(Collectors.toList());

        List<SearchResultItem> items = new ArrayList<>();
        for (int i = 0; i < topDocs.size(); i++) {
            ScoredDocument doc = topDocs.get(i);
            CitedAnswer cited = null;
            if (request.isIncludeCitations() && doc.getContent() != null) {
                cited = citationGenerator.generateCitations(
                        query + "\n\n" + doc.getContent(), Collections.singletonList(doc));
            }
            String annotatedContent = doc.getContent();
            if (cited != null && !cited.getCitations().isEmpty()) {
                annotatedContent = citationGenerator.annotateAnswerWithCitations(
                        doc.getContent(), cited.getCitations());
            }
            items.add(SearchResultItem.builder()
                    .rank(i + 1).content(doc.getContent()).title(doc.getTitle())
                    .score(doc.getScore())
                    .citations(cited != null ? cited.getCitations() : Collections.emptyList())
                    .annotatedContent(annotatedContent).build());
        }

        metrics.recordSearch(query, hybridResult.getRetrievalTimeMs(), rerankTime,
                hybridResult.getTotalCandidates(), items.size(), fromCache);

        return ResponseEntity.ok(SearchResponse.builder()
                .query(query).results(items)
                .metadata(SearchMetadata.builder()
                        .retrievalTimeMs(hybridResult.getRetrievalTimeMs())
                        .rerankTimeMs(rerankTime)
                        .totalCandidates(hybridResult.getTotalCandidates())
                        .finalCount(items.size())
                        .averageConfidence(rerankedResult.getAverageConfidence())
                        .retrievalMethod("hybrid_rrf_cross_encoder").build())
                .fromCache(fromCache).build());
    }

    @PostMapping("/answer")
    public ResponseEntity<AnswerResponse> searchWithAnswer(@RequestBody SearchRequest request) {
        String query = request.getQuery();
        int topK = request.getTopK() > 0 ? request.getTopK() : 5;

        Optional<GeneratedAnswer> cachedAnswer = semanticCache.getAnswer(query);
        if (cachedAnswer.isPresent()) {
            GeneratedAnswer cached = cachedAnswer.get();
            metrics.recordAnswer(query, 0, 0, cached.getTokensUsed(), cached.getConfidence(), true);
            return ResponseEntity.ok(AnswerResponse.builder()
                    .query(query).answer(cached.getAnswer()).citations(cached.getCitations())
                    .confidence(cached.getConfidence()).model(cached.getModel())
                    .retrievalTimeMs(0).generationTimeMs(0).fromCache(true).build());
        }

        long retrievalStart = System.currentTimeMillis();
        HybridResult hybridResult = hybridRetriever.searchWithIntent(query);
        long retrievalTime = System.currentTimeMillis() - retrievalStart;

        RerankedResult reranked = reranker.rerank(query, hybridResult.getDocuments());
        List<ScoredDocument> topDocs = reranked.getDocuments().stream()
                .limit(topK).collect(Collectors.toList());
        semanticCache.putRetrieval(query, hybridResult);

        long generationStart = System.currentTimeMillis();
        GeneratedAnswer generated = answerGenerator.generate(query, topDocs);
        long generationTime = System.currentTimeMillis() - generationStart;

        semanticCache.putAnswer(query, generated);
        CompressedContext compressed = contextCompressor.compress(query, topDocs);

        metrics.recordAnswer(query, retrievalTime, generationTime,
                generated.getTokensUsed(), generated.getConfidence(), false);

        return ResponseEntity.ok(AnswerResponse.builder()
                .query(query).answer(generated.getAnswer()).citations(generated.getCitations())
                .confidence(generated.getConfidence()).model(generated.getModel())
                .retrievalTimeMs(retrievalTime).generationTimeMs(generationTime)
                .compressionRatio(compressed.getCompressionRatio())
                .originalTokens(compressed.getOriginalTokens())
                .compressedTokens(compressed.getCompressedTokens())
                .fromCache(false).build());
    }

    @GetMapping("/quick")
    public ResponseEntity<SearchResponse> quickSearch(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "5") int limit) {

        boolean fromCache = false;
        Optional<HybridResult> cached = semanticCache.getRetrieval(query);
        HybridResult hybridResult;

        if (cached.isPresent()) {
            hybridResult = cached.get();
            fromCache = true;
        } else {
            hybridResult = hybridRetriever.searchWithIntent(query);
            semanticCache.putRetrieval(query, hybridResult);
        }

        List<ScoredDocument> topDocs = hybridResult.getDocuments().stream()
                .limit(limit).collect(Collectors.toList());

        List<SearchResultItem> items = topDocs.stream()
                .map(doc -> SearchResultItem.builder()
                        .rank(topDocs.indexOf(doc) + 1).content(doc.getContent())
                        .title(doc.getTitle()).score(doc.getScore()).build())
                .collect(Collectors.toList());

        metrics.recordSearch(query, hybridResult.getRetrievalTimeMs(), 0,
                hybridResult.getTotalCandidates(), items.size(), fromCache);

        return ResponseEntity.ok(SearchResponse.builder()
                .query(query).results(items)
                .metadata(SearchMetadata.builder()
                        .retrievalTimeMs(hybridResult.getRetrievalTimeMs())
                        .totalCandidates(hybridResult.getTotalCandidates())
                        .finalCount(items.size()).retrievalMethod("hybrid_quick").build())
                .fromCache(fromCache).build());
    }

    // ==================== NEW: RAG-SPECIFIC ENDPOINTS ====================

    /**
     * Debug search endpoint - returns detailed retrieval results with scores,
     * titles, document IDs, and content previews. Uses RagSearchService for
     * improved retrieval with metadata-rich context.
     */
    @PostMapping("/debug-search")
    public ResponseEntity<?> debugSearch(@RequestBody Map<String, Object> request) {
        try {
            String query = (String) request.get("query");
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Query is required"));
            }

            List<Map<String, Object>> debugResults = ragSearchService.searchDebug(query);
            Map<String, Object> stats = ragSearchService.getSearchStats(query);

            // Transform results into structured DTOs
            List<DebugResultItem> items = debugResults.stream()
                    .map(r -> DebugResultItem.builder()
                            .score(r.get("score") instanceof Number ? ((Number) r.get("score")).doubleValue() : 0.0)
                            .title(r.get("title") != null ? r.get("title").toString() : "Unknown")
                            .documentId(r.get("document_id") != null ? r.get("document_id").toString() : "N/A")
                            .contentPreview(r.get("content_preview") != null ? r.get("content_preview").toString() : "")
                            .category(r.get("category") != null ? r.get("category").toString() : null)
                            .subcategory(r.get("subcategory") != null ? r.get("subcategory").toString() : null)
                            .stage(r.get("stage") != null ? r.get("stage").toString() : null)
                            .build())
                    .collect(Collectors.toList());

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("query", query);
            response.put("total_results", items.size());
            response.put("results", items);
            response.put("stats", stats);
            response.put("max_score", stats.get("max_score"));
            response.put("avg_score", stats.get("avg_score"));
            response.put("high_quality_results", stats.get("high_quality_results"));

            metrics.recordSearch(query, 0, 0, items.size(), items.size(), false);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Debug search failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Rich context search endpoint - returns formatted context with full metadata
     * ready for LLM injection.
     */
    @PostMapping("/search-with-context")
    public ResponseEntity<?> searchWithContext(@RequestBody Map<String, Object> request) {
        try {
            String query = (String) request.get("query");
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Query is required"));
            }

            int topK = request.containsKey("topK") ? ((Number) request.get("topK")).intValue() : 10;

            String context = ragSearchService.searchWithContext(query);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("query", query);
            response.put("context", context);
            response.put("answer", context);
            response.put("confidence", 0.85);
            response.put("model", "DeepSeek V3 + RAG");
            response.put("citations", List.of());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Search with context failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Search statistics endpoint - returns retrieval quality metrics for
     * monitoring.
     */
    @PostMapping("/search-stats")
    public ResponseEntity<?> getSearchStats(@RequestBody Map<String, Object> request) {
        try {
            String query = (String) request.get("query");
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Query is required"));
            }

            Map<String, Object> stats = ragSearchService.getSearchStats(query);
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Failed to get search stats");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Keyword-boosted search endpoint.
     */
    @PostMapping("/search-with-keywords")
    public ResponseEntity<?> searchWithKeywords(@RequestBody Map<String, Object> request) {
        try {
            String query = (String) request.get("query");
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Query is required"));
            }

            @SuppressWarnings("unchecked")
            List<String> keywords = (List<String>) request.getOrDefault("keywords", List.of());

            String context = ragSearchService.searchWithKeywords(query, keywords);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("query", query);
            response.put("keywords", keywords);
            response.put("answer", context);
            response.put("confidence", 0.85);
            response.put("model", "DeepSeek V3 + RAG");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Keyword search failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Metadata-filtered search endpoint.
     */
    @PostMapping("/search-with-metadata")
    public ResponseEntity<?> searchWithMetadataFilter(@RequestBody Map<String, Object> request) {
        try {
            String query = (String) request.get("query");
            if (query == null || query.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Query is required"));
            }

            @SuppressWarnings("unchecked")
            Map<String, String> metadata = (Map<String, String>) request.getOrDefault("metadata", Map.of());

            String context = ragSearchService.searchWithMetadataFilter(query, metadata);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("query", query);
            response.put("metadata", metadata);
            response.put("answer", context);
            response.put("confidence", 0.85);
            response.put("model", "DeepSeek V3 + RAG");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "Metadata-filtered search failed");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    // ==================== KNOWLEDGE GRAPH ENDPOINTS ====================

    @GetMapping("/graph/related")
    public ResponseEntity<Map<String, Object>> getRelatedConcepts(
            @RequestParam String concept, @RequestParam(defaultValue = "2") int depth) {
        List<KnowledgeGraphNode> related = knowledgeGraph.getRelatedConcepts(concept, depth);
        List<KnowledgeGraphEdge> edges = knowledgeGraph.getAllEdges().stream()
                .filter(e -> related.stream().anyMatch(n -> n.getId().equals(e.getSourceNodeId())
                        || n.getId().equals(e.getTargetNodeId())))
                .collect(Collectors.toList());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("concept", concept);
        response.put("depth", depth);
        response.put("nodes", related);
        response.put("edges", edges);
        response.put("totalRelated", related.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/graph/path")
    public ResponseEntity<Map<String, Object>> getGraphPath(
            @RequestParam String from, @RequestParam String to) {
        List<KnowledgeGraphEdge> path = knowledgeGraph.getPath(from, to);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("from", from);
        response.put("to", to);
        response.put("path", path);
        response.put("pathLength", path.size());
        response.put("pathFound", !path.isEmpty());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/graph/expand")
    public ResponseEntity<Map<String, Object>> expandQuery(@RequestParam String query) {
        List<String> expanded = knowledgeGraph.expandQuery(query);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("original", query);
        response.put("expanded", expanded);
        response.put("expansionCount", expanded.size() - 1);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/graph/domain")
    public ResponseEntity<Map<String, Object>> getDomainConcepts(@RequestParam String domain) {
        List<KnowledgeGraphNode> concepts = knowledgeGraph.getDomainConcepts(domain);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("domain", domain);
        response.put("concepts", concepts);
        response.put("totalConcepts", concepts.size());
        return ResponseEntity.ok(response);
    }

    // ==================== CACHE ENDPOINTS ====================

    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> cacheStats() {
        return ResponseEntity.ok(semanticCache.getStats());
    }

    @DeleteMapping("/cache")
    public ResponseEntity<Map<String, String>> clearCache() {
        semanticCache.clear();
        return ResponseEntity.ok(Map.of("status", "cache cleared"));
    }

    // ==================== CONVERSATION ENDPOINTS ====================

    @PostMapping("/conversation/start")
    public ResponseEntity<Map<String, Object>> startConversation() {
        String sessionId = UUID.randomUUID().toString();
        conversationMemory.createSession(sessionId);
        return ResponseEntity.ok(Map.of("sessionId", sessionId, "status", "created"));
    }

    @PostMapping("/conversation/ask")
    public ResponseEntity<AnswerResponse> conversationalSearch(
            @RequestBody Map<String, Object> requestBody) {
        String query = (String) requestBody.get("query");
        String sessionId = (String) requestBody.get("sessionId");
        int topK = requestBody.containsKey("topK") ? ((Number) requestBody.get("topK")).intValue() : 5;

        String enhancedQuery = query;
        if (sessionId != null) {
            enhancedQuery = conversationMemory.enhanceQueryWithContext(sessionId, query);
        }

        HybridResult hybridResult = hybridRetriever.searchWithIntent(enhancedQuery);
        RerankedResult reranked = reranker.rerank(enhancedQuery, hybridResult.getDocuments());
        List<ScoredDocument> topDocs = reranked.getDocuments().stream()
                .limit(topK).collect(Collectors.toList());

        GeneratedAnswer generated = answerGenerator.generate(enhancedQuery, topDocs);

        if (sessionId != null) {
            conversationMemory.addTurn(sessionId, "user", query, null, null);
            conversationMemory.addTurn(sessionId, "assistant", generated.getAnswer(),
                    generated.getCitations().stream().map(c -> c.getSourceTitle())
                            .collect(Collectors.toList()),
                    null);
        }

        return ResponseEntity.ok(AnswerResponse.builder()
                .query(query).answer(generated.getAnswer()).citations(generated.getCitations())
                .confidence(generated.getConfidence()).model(generated.getModel())
                .retrievalTimeMs(hybridResult.getRetrievalTimeMs()).generationTimeMs(0L)
                .fromCache(false).build());
    }

    @GetMapping("/conversation/{sessionId}")
    public ResponseEntity<Map<String, Object>> getConversation(@PathVariable String sessionId) {
        ConversationMemory.ConversationContext context = conversationMemory.getContext(sessionId, "");
        Map<String, Object> stats = conversationMemory.getSessionStats(sessionId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sessionId", sessionId);
        response.put("recentTurns", context.getRecentTurns());
        response.put("totalTurns", context.getTotalTurns());
        response.put("isCompressed", context.isCompressed());
        response.put("stats", stats);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/conversation/{sessionId}")
    public ResponseEntity<Map<String, String>> deleteConversation(@PathVariable String sessionId) {
        conversationMemory.deleteSession(sessionId);
        return ResponseEntity.ok(Map.of("status", "deleted", "sessionId", sessionId));
    }

    @GetMapping("/conversation/stats")
    public ResponseEntity<Map<String, Object>> conversationStats() {
        return ResponseEntity.ok(conversationMemory.getGlobalStats());
    }

    // ==================== METRICS ENDPOINTS ====================

    @GetMapping("/metrics/dashboard")
    public ResponseEntity<MetricsCollector.DashboardMetrics> dashboard() {
        return ResponseEntity.ok(metrics.getDashboard());
    }

    @PostMapping("/metrics/reset")
    public ResponseEntity<Map<String, String>> resetMetrics() {
        metrics.reset();
        return ResponseEntity.ok(Map.of("status", "metrics reset"));
    }

    // ==================== HEALTH ====================
    // Add this to SearchController.java

    @GetMapping("/model")
    public ResponseEntity<Map<String, String>> getActiveModel() {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("model", "google/gemini-2.0-flash-exp:free");
        response.put("status", "active");
        response.put("tier", "free");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "operational");
        health.put("pipeline", "hybrid_rrf_cross_encoder_semantic_cache_graph_conversation_observability_rag_service");
        health.put("version", "2.4.0");
        health.put("graphNodes", knowledgeGraph.getAllNodes().size());
        health.put("graphEdges", knowledgeGraph.getAllEdges().size());
        health.putAll(semanticCache.getStats());
        health.putAll(conversationMemory.getGlobalStats());
        health.put("totalSearches", metrics.getDashboard().getTotalSearches());
        health.put("cacheHitRate", String.format("%.2f%%", metrics.getDashboard().getCacheHitRate() * 100));
        return ResponseEntity.ok(health);
    }
}