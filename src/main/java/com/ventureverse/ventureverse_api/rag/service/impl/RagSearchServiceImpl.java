package com.ventureverse.ventureverse_api.rag.service.impl;

import com.ventureverse.ventureverse_api.rag.service.EmbeddingService;
import com.ventureverse.ventureverse_api.rag.service.QdrantRestService;
import com.ventureverse.ventureverse_api.rag.service.RagSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagSearchServiceImpl implements RagSearchService {

        private final EmbeddingService embeddingService;
        private final QdrantRestService qdrantRestService;

        // -------------------------------------------------------------------------
        // Constants
        // -------------------------------------------------------------------------

        /** Default number of chunks to retrieve for RAG */
        private static final int DEFAULT_SEARCH_LIMIT = 10;

        /** Minimum relevance score to include a result in the context */
        private static final double MIN_RELEVANCE_SCORE = 0.50;

        /**
         * Maximum number of chunks to include in the final context (after filtering)
         */
        private static final int MAX_CONTEXT_CHUNKS = 8;

        // -------------------------------------------------------------------------
        // Legacy Search (Backward Compatible)
        // -------------------------------------------------------------------------

        /**
         * @deprecated Use {@link #searchWithContext(String)} for rich context with
         *             metadata.
         *             This method now retrieves 10 chunks instead of 5.
         */
        @Override
        @Deprecated
        public List<String> search(String query) {
                var vector = embeddingService.createEmbedding(query);

                // Use searchWithScores with improved limit of 10
                List<Map<String, Object>> scoredResults = qdrantRestService.searchWithScores(vector,
                                DEFAULT_SEARCH_LIMIT);

                return scoredResults.stream()
                                .map(r -> {
                                        Map<?, ?> payload = (Map<?, ?>) r.get("payload");
                                        Object content = payload.get("content");
                                        return content != null ? content.toString() : "";
                                })
                                .filter(s -> !s.isEmpty())
                                .collect(Collectors.toList());
        }

        // -------------------------------------------------------------------------
        // Rich Context Search (Primary RAG Method)
        // -------------------------------------------------------------------------

        /**
         * Searches the knowledge base and returns formatted context with metadata,
         * ready for injection into LLM prompts. Results are filtered by relevance score
         * and capped at MAX_CONTEXT_CHUNKS.
         *
         * @param query The user's query string
         * @return Formatted context string with document titles, IDs, categories, and
         *         relevance scores
         */
        @Override
        public String searchWithContext(String query) {
                var vector = embeddingService.createEmbedding(query);

                // Retrieve rich context with metadata
                List<Map<String, Object>> richResults = qdrantRestService.searchWithRichContext(vector,
                                DEFAULT_SEARCH_LIMIT);

                // Filter by minimum relevance score - safe type casting
                List<Map<String, Object>> filteredResults = richResults.stream()
                                .filter(r -> {
                                        Object scoreObj = r.get("score");
                                        double score = 0.0;
                                        if (scoreObj instanceof Number) {
                                                score = ((Number) scoreObj).doubleValue();
                                        }
                                        return score >= MIN_RELEVANCE_SCORE;
                                })
                                .limit(MAX_CONTEXT_CHUNKS)
                                .collect(Collectors.toList());

                // Build consolidated context string
                return buildContextFromResults(filteredResults);
        }

        // -------------------------------------------------------------------------
        // Debug Search (Retrieval Quality Monitoring)
        // -------------------------------------------------------------------------

        /**
         * Returns detailed search results including scores, titles, and document IDs
         * for debugging and monitoring retrieval quality. Retrieves 15 results for
         * better visibility into what the vector search is returning.
         *
         * @param query The user's query string
         * @return List of maps containing score, title, document_id, and content
         *         preview
         */
        @Override
        public List<Map<String, Object>> searchDebug(String query) {
                var vector = embeddingService.createEmbedding(query);

                // Retrieve 15 results for debugging visibility
                List<Map<String, Object>> richResults = qdrantRestService.searchWithRichContext(vector, 15);

                List<Map<String, Object>> debugResults = new ArrayList<>();
                for (Map<String, Object> result : richResults) {
                        Map<String, Object> debugEntry = new LinkedHashMap<>();
                        debugEntry.put("score", result.get("score"));
                        debugEntry.put("title", result.get("title"));
                        debugEntry.put("document_id", result.get("document_id"));

                        // Include content preview (first 200 characters)
                        Object contextObj = result.get("context");
                        String context = contextObj != null ? contextObj.toString() : "";
                        if (context.length() > 200) {
                                debugEntry.put("content_preview", context.substring(0, 200) + "...");
                        } else {
                                debugEntry.put("content_preview", context);
                        }

                        // Include additional metadata if available
                        Map<?, ?> payload = (Map<?, ?>) result.get("payload");
                        if (payload != null) {
                                if (payload.get("category") != null) {
                                        debugEntry.put("category", payload.get("category"));
                                }
                                if (payload.get("subcategory") != null) {
                                        debugEntry.put("subcategory", payload.get("subcategory"));
                                }
                                if (payload.get("stage") != null) {
                                        debugEntry.put("stage", payload.get("stage"));
                                }
                        }

                        debugResults.add(debugEntry);
                }
                return debugResults;
        }

        // -------------------------------------------------------------------------
        // Keyword-Boosted Search
        // -------------------------------------------------------------------------

        /**
         * Searches with keyword boosting for queries that contain specific terms
         * that should be weighted more heavily.
         *
         * @param query    The user's query string
         * @param keywords List of keywords to boost
         * @return Formatted context string
         */
        @Override
        public String searchWithKeywords(String query, List<String> keywords) {
                var vector = embeddingService.createEmbedding(query);

                List<Map<String, Object>> results = qdrantRestService.searchWithKeywordBoost(vector, keywords,
                                DEFAULT_SEARCH_LIMIT);

                List<Map<String, Object>> richResults = new ArrayList<>();
                for (Map<String, Object> result : results) {
                        Map<?, ?> payload = (Map<?, ?>) result.get("payload");
                        double score = getScoreSafe(result);

                        Map<String, Object> richResult = new LinkedHashMap<>();
                        richResult.put("id", result.get("id"));
                        richResult.put("score", score);
                        richResult.put("title", payload.get("title"));
                        richResult.put("document_id", payload.get("document_id"));

                        StringBuilder context = new StringBuilder();
                        context.append("TITLE: ").append(payload.get("title")).append("\n");
                        context.append("DOCUMENT_ID: ").append(payload.get("document_id")).append("\n");
                        context.append("RELEVANCE_SCORE: ").append(String.format("%.4f", score)).append("\n\n");
                        context.append(payload.get("content") != null ? payload.get("content").toString() : "");

                        richResult.put("context", context.toString());
                        richResult.put("payload", payload);
                        richResults.add(richResult);
                }

                // Filter and limit
                List<Map<String, Object>> filteredResults = richResults.stream()
                                .filter(r -> getScoreSafe(r) >= MIN_RELEVANCE_SCORE)
                                .limit(MAX_CONTEXT_CHUNKS)
                                .collect(Collectors.toList());

                return buildContextFromResults(filteredResults);
        }

        // -------------------------------------------------------------------------
        // Metadata-Filtered Search
        // -------------------------------------------------------------------------

        /**
         * Searches with metadata filtering (e.g., by category, stage, or personas).
         *
         * @param query    The user's query string
         * @param metadata Map of metadata fields to filter on
         * @return Formatted context string
         */
        @Override
        public String searchWithMetadataFilter(String query, Map<String, String> metadata) {
                var vector = embeddingService.createEmbedding(query);

                List<Map<String, Object>> results = qdrantRestService.searchWithMetadataFilter(vector,
                                DEFAULT_SEARCH_LIMIT, metadata);

                List<Map<String, Object>> richResults = new ArrayList<>();
                for (Map<String, Object> result : results) {
                        Map<?, ?> payload = (Map<?, ?>) result.get("payload");
                        double score = getScoreSafe(result);

                        Map<String, Object> richResult = new LinkedHashMap<>();
                        richResult.put("id", result.get("id"));
                        richResult.put("score", score);
                        richResult.put("title", payload.get("title"));
                        richResult.put("document_id", payload.get("document_id"));

                        StringBuilder context = new StringBuilder();
                        context.append("TITLE: ").append(payload.get("title")).append("\n");
                        context.append("DOCUMENT_ID: ").append(payload.get("document_id")).append("\n");
                        if (payload.get("category") != null) {
                                context.append("CATEGORY: ").append(payload.get("category")).append("\n");
                        }
                        context.append("RELEVANCE_SCORE: ").append(String.format("%.4f", score)).append("\n\n");
                        context.append(payload.get("content") != null ? payload.get("content").toString() : "");

                        richResult.put("context", context.toString());
                        richResult.put("payload", payload);
                        richResults.add(richResult);
                }

                List<Map<String, Object>> filteredResults = richResults.stream()
                                .filter(r -> getScoreSafe(r) >= MIN_RELEVANCE_SCORE)
                                .limit(MAX_CONTEXT_CHUNKS)
                                .collect(Collectors.toList());

                return buildContextFromResults(filteredResults);
        }

        // -------------------------------------------------------------------------
        // Search Statistics
        // -------------------------------------------------------------------------

        /**
         * Returns statistics about the last search for monitoring purposes.
         */
        @Override
        public Map<String, Object> getSearchStats(String query) {
                var vector = embeddingService.createEmbedding(query);
                List<Map<String, Object>> results = qdrantRestService.searchWithRichContext(vector,
                                DEFAULT_SEARCH_LIMIT);

                Map<String, Object> stats = new LinkedHashMap<>();
                stats.put("query", query);
                stats.put("total_results", results.size());

                if (!results.isEmpty()) {
                        double maxScore = results.stream()
                                        .mapToDouble(r -> getScoreSafe(r))
                                        .max()
                                        .orElse(0.0);
                        double avgScore = results.stream()
                                        .mapToDouble(r -> getScoreSafe(r))
                                        .average()
                                        .orElse(0.0);
                        long highQualityResults = results.stream()
                                        .filter(r -> getScoreSafe(r) >= MIN_RELEVANCE_SCORE)
                                        .count();

                        stats.put("max_score", String.format("%.4f", maxScore));
                        stats.put("avg_score", String.format("%.4f", avgScore));
                        stats.put("high_quality_results", highQualityResults);
                        stats.put("search_limit", DEFAULT_SEARCH_LIMIT);
                        stats.put("min_relevance_threshold", MIN_RELEVANCE_SCORE);

                        // Top 5 titles
                        List<String> topTitles = results.stream()
                                        .limit(5)
                                        .map(r -> (String) r.get("title"))
                                        .collect(Collectors.toList());
                        stats.put("top_titles", topTitles);
                }

                return stats;
        }

        // -------------------------------------------------------------------------
        // Private Helper Methods
        // -------------------------------------------------------------------------

        /**
         * Safely extracts a double score from a result map, handling both
         * Double and Integer types that Qdrant may return.
         */
        private double getScoreSafe(Map<String, Object> result) {
                Object scoreObj = result.get("score");
                if (scoreObj instanceof Number) {
                        return ((Number) scoreObj).doubleValue();
                }
                return 0.0;
        }

        /**
         * Builds a consolidated context string from search results.
         */
        private String buildContextFromResults(List<Map<String, Object>> results) {
                if (results.isEmpty()) {
                        return "No relevant documents found in the knowledge base.";
                }

                StringBuilder context = new StringBuilder();
                context.append("RETRIEVED KNOWLEDGE BASE DOCUMENTS\n");
                context.append("====================================\n\n");

                for (int i = 0; i < results.size(); i++) {
                        Map<String, Object> result = results.get(i);
                        context.append("--- Source ").append(i + 1).append(" ---\n");
                        Object contextObj = result.get("context");
                        context.append(contextObj != null ? contextObj.toString() : "");
                        context.append("\n\n");
                }

                return context.toString();
        }
}