package com.ventureverse.ventureverse_api.rag.service.impl;

import com.ventureverse.ventureverse_api.rag.config.QdrantConfig;
import com.ventureverse.ventureverse_api.rag.service.QdrantRestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QdrantRestServiceImpl implements QdrantRestService {

        private final QdrantConfig config;
        private final RestTemplate restTemplate;
        private static final String COLLECTION_NAME = "ventureverse_knowledge";

        // -------------------------------------------------------------------------
        // Constants
        // -------------------------------------------------------------------------

        private static final int DEFAULT_SEARCH_LIMIT = 12;
        private static final int MAX_SEARCH_LIMIT = 25;
        private static final int MIN_SEARCH_LIMIT = 3;
        private static final double DEFAULT_KEYWORD_BOOST = 0.15;
        private static final double HIGH_SCORE_THRESHOLD = 0.75;

        // -------------------------------------------------------------------------
        // Collection Management
        // -------------------------------------------------------------------------

        @Override
        public void createCollection() {
                String url = config.getQdrantUrl() + "/collections/" + COLLECTION_NAME;
                HttpHeaders headers = createHeaders();

                Map<String, Object> vectors = new HashMap<>();
                vectors.put("size", 1024);
                vectors.put("distance", "Cosine");

                Map<String, Object> body = new HashMap<>();
                body.put("vectors", vectors);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

                try {
                        restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
                        System.out.println("Qdrant collection '" + COLLECTION_NAME + "' ready");

                        createPayloadIndex("document_id", "keyword");
                        createPayloadIndex("title", "text");
                        createPayloadIndex("category", "keyword");
                        createPayloadIndex("subcategory", "keyword");
                        createPayloadIndex("stage", "keyword");
                        createPayloadIndex("personas", "keyword");
                        createPayloadIndex("difficulty", "keyword");

                } catch (Exception e) {
                        System.out.println("Collection already exists or creation failed: " + e.getMessage());
                }
        }

        private void createPayloadIndex(String fieldName, String fieldType) {
                try {
                        String url = config.getQdrantUrl() + "/collections/" + COLLECTION_NAME + "/index";
                        HttpHeaders headers = createHeaders();

                        Map<String, Object> body = new HashMap<>();
                        body.put("field_name", fieldName);
                        body.put("field_schema", fieldType);

                        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                        restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
                        System.out.println("Payload index created: " + fieldName + " (" + fieldType + ")");
                } catch (Exception e) {
                        System.out.println("Payload index for '" + fieldName + "' already exists: " + e.getMessage());
                }
        }

        // -------------------------------------------------------------------------
        // Basic Search (Backward Compatible)
        // -------------------------------------------------------------------------

        @Override
        @Deprecated
        public List<String> search(List<Float> vector) {
                try {
                        List<Map<String, Object>> results = searchWithScores(vector, DEFAULT_SEARCH_LIMIT);
                        return results.stream()
                                        .map(r -> {
                                                Map<?, ?> payload = (Map<?, ?>) r.get("payload");
                                                Object content = payload.get("content");
                                                return content != null ? content.toString() : "";
                                        })
                                        .filter(s -> !s.isEmpty())
                                        .collect(Collectors.toList());
                } catch (Exception e) {
                        throw new RuntimeException("Qdrant search failed", e);
                }
        }

        // -------------------------------------------------------------------------
        // Search with Scores
        // -------------------------------------------------------------------------

        @Override
        public List<Map<String, Object>> searchWithScores(List<Float> vector, int limit) {
                limit = clampLimit(limit);
                String url = config.getQdrantUrl() + "/collections/" + COLLECTION_NAME + "/points/search";
                HttpHeaders headers = createHeaders();

                Map<String, Object> body = new HashMap<>();
                body.put("vector", vector);
                body.put("limit", limit);
                body.put("with_payload", true);
                body.put("with_vector", false);

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                                url, HttpMethod.POST, request,
                                (Class<Map<String, Object>>) (Class<?>) Map.class);

                List<Map<String, Object>> results = new ArrayList<>();
                List<?> resultList = (List<?>) response.getBody().get("result");

                if (resultList != null) {
                        for (Object obj : resultList) {
                                Map<?, ?> point = (Map<?, ?>) obj;
                                Map<String, Object> scoredResult = new HashMap<>();
                                scoredResult.put("id", point.get("id"));
                                scoredResult.put("score", point.get("score"));
                                scoredResult.put("payload", point.get("payload"));
                                results.add(scoredResult);
                        }
                }
                return results;
        }

        // -------------------------------------------------------------------------
        // Rich Context Search (NEW - Fixes Compilation Error #1 and #3)
        // -------------------------------------------------------------------------

        /**
         * Searches Qdrant and returns formatted context strings with metadata,
         * ready to be injected into LLM prompts. High-scoring results include
         * full metadata; lower-scoring results include basic context.
         *
         * @param vector The query embedding vector
         * @param limit  Number of results to retrieve
         * @return List of maps containing id, score, title, document_id, context, and
         *         payload
         */
        @Override
        public List<Map<String, Object>> searchWithRichContext(List<Float> vector, int limit) {
                List<Map<String, Object>> results = searchWithScores(vector, limit);
                List<Map<String, Object>> richResults = new ArrayList<>();

                for (Map<String, Object> result : results) {
                        Map<?, ?> payload = (Map<?, ?>) result.get("payload");
                        double score = (double) result.get("score");

                        Map<String, Object> richResult = new LinkedHashMap<>();
                        richResult.put("id", result.get("id"));
                        richResult.put("score", score);

                        // Build formatted context with metadata
                        StringBuilder context = new StringBuilder();

                        // Include title and document ID for all results
                        String title = payload.get("title") != null ? payload.get("title").toString() : "Untitled";
                        String documentId = payload.get("document_id") != null ? payload.get("document_id").toString()
                                        : "N/A";

                        context.append("TITLE: ").append(title).append("\n");
                        context.append("DOCUMENT_ID: ").append(documentId).append("\n");

                        // Include additional metadata for high-scoring results
                        if (score >= HIGH_SCORE_THRESHOLD) {
                                if (payload.get("category") != null) {
                                        context.append("CATEGORY: ").append(payload.get("category")).append("\n");
                                }
                                if (payload.get("subcategory") != null) {
                                        context.append("SUBCATEGORY: ").append(payload.get("subcategory")).append("\n");
                                }
                                if (payload.get("stage") != null) {
                                        context.append("STAGE: ").append(payload.get("stage")).append("\n");
                                }
                        }

                        context.append("RELEVANCE_SCORE: ").append(String.format("%.4f", score)).append("\n");
                        context.append("\n");
                        context.append(payload.get("content") != null ? payload.get("content").toString() : "");

                        richResult.put("context", context.toString());
                        richResult.put("title", title);
                        richResult.put("document_id", documentId);
                        richResult.put("score", score);
                        richResult.put("payload", payload);

                        richResults.add(richResult);
                }
                return richResults;
        }

        // -------------------------------------------------------------------------
        // Keyword-Boosted Search
        // -------------------------------------------------------------------------

        @Override
        public List<Map<String, Object>> searchWithKeywordBoost(List<Float> vector, List<String> keywords, int limit) {
                return searchWithKeywordBoost(vector, keywords, limit, DEFAULT_KEYWORD_BOOST);
        }

        public List<Map<String, Object>> searchWithKeywordBoost(
                        List<Float> vector, List<String> keywords, int limit, double boostWeight) {

                limit = clampLimit(limit);
                String url = config.getQdrantUrl() + "/collections/" + COLLECTION_NAME + "/points/search";
                HttpHeaders headers = createHeaders();

                List<Map<String, Object>> shouldConditions = new ArrayList<>();
                for (String keyword : keywords) {
                        Map<String, Object> condition = new HashMap<>();
                        condition.put("key", "content");
                        Map<String, Object> match = new HashMap<>();
                        match.put("text", keyword);
                        condition.put("match", match);
                        shouldConditions.add(condition);
                }

                Map<String, Object> keywordFilter = new HashMap<>();
                keywordFilter.put("should", shouldConditions);

                Map<String, Object> body = new HashMap<>();
                body.put("vector", vector);
                body.put("limit", limit);
                body.put("with_payload", true);
                body.put("with_vector", false);

                if (!keywords.isEmpty()) {
                        body.put("filter", keywordFilter);
                }

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                                url, HttpMethod.POST, request,
                                (Class<Map<String, Object>>) (Class<?>) Map.class);

                List<Map<String, Object>> results = new ArrayList<>();
                List<?> resultList = (List<?>) response.getBody().get("result");

                if (resultList != null) {
                        for (Object obj : resultList) {
                                Map<?, ?> point = (Map<?, ?>) obj;
                                Map<String, Object> scoredResult = new HashMap<>();
                                scoredResult.put("id", point.get("id"));
                                scoredResult.put("score", point.get("score"));
                                scoredResult.put("payload", point.get("payload"));
                                results.add(scoredResult);
                        }
                }
                return results;
        }

        // -------------------------------------------------------------------------
        // Metadata-Filtered Search (NEW - Fixes Compilation Error #2)
        // -------------------------------------------------------------------------

        /**
         * Searches Qdrant with metadata filtering (e.g., by category, stage, personas).
         *
         * @param vector   The query embedding vector
         * @param limit    Number of results to retrieve
         * @param metadata Map of metadata fields to filter on (e.g., "category":
         *                 "Venture Capital")
         * @return List of results with scores and payloads
         */
        @Override
        public List<Map<String, Object>> searchWithMetadataFilter(
                        List<Float> vector, int limit, Map<String, String> metadata) {

                limit = clampLimit(limit);
                String url = config.getQdrantUrl() + "/collections/" + COLLECTION_NAME + "/points/search";
                HttpHeaders headers = createHeaders();

                // Build must conditions from metadata
                List<Map<String, Object>> mustConditions = new ArrayList<>();
                for (Map.Entry<String, String> entry : metadata.entrySet()) {
                        Map<String, Object> condition = new HashMap<>();
                        condition.put("key", entry.getKey());
                        Map<String, Object> match = new HashMap<>();
                        match.put("value", entry.getValue());
                        condition.put("match", match);
                        mustConditions.add(condition);
                }

                Map<String, Object> body = new HashMap<>();
                body.put("vector", vector);
                body.put("limit", limit);
                body.put("with_payload", true);
                body.put("with_vector", false);

                if (!metadata.isEmpty()) {
                        Map<String, Object> filter = new HashMap<>();
                        filter.put("must", mustConditions);
                        body.put("filter", filter);
                }

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                                url, HttpMethod.POST, request,
                                (Class<Map<String, Object>>) (Class<?>) Map.class);

                List<Map<String, Object>> results = new ArrayList<>();
                List<?> resultList = (List<?>) response.getBody().get("result");

                if (resultList != null) {
                        for (Object obj : resultList) {
                                Map<?, ?> point = (Map<?, ?>) obj;
                                Map<String, Object> scoredResult = new HashMap<>();
                                scoredResult.put("id", point.get("id"));
                                scoredResult.put("score", point.get("score"));
                                scoredResult.put("payload", point.get("payload"));
                                results.add(scoredResult);
                        }
                }
                return results;
        }

        // -------------------------------------------------------------------------
        // Vector Insertion Methods
        // -------------------------------------------------------------------------

        @Override
        @Deprecated
        public void insertVector(String id, String title, String content, List<Float> vector) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("document_id", id);
                insertVectorWithMetadata(id, title, content, vector, metadata);
        }

        @Override
        public void insertVectorWithMetadata(String id, String title, String content, List<Float> vector,
                        Map<String, Object> metadata) {
                String url = config.getQdrantUrl() + "/collections/" + COLLECTION_NAME + "/points";
                HttpHeaders headers = createHeaders();

                Map<String, Object> payload = new HashMap<>();
                payload.put("title", title);
                payload.put("content", content);
                payload.putAll(metadata);

                Map<String, Object> point = new HashMap<>();
                point.put("id", id);
                point.put("vector", vector);
                point.put("payload", payload);

                Map<String, Object> body = new HashMap<>();
                body.put("points", List.of(point));

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                restTemplate.exchange(url, HttpMethod.PUT, request, String.class);
                System.out.println("Inserted document into Qdrant: " + id + " - " + title);
        }

        // -------------------------------------------------------------------------
        // Helper Methods
        // -------------------------------------------------------------------------

        private HttpHeaders createHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.set("api-key", config.getQdrantApiKey());
                headers.setContentType(MediaType.APPLICATION_JSON);
                return headers;
        }

        private int clampLimit(int limit) {
                return Math.max(MIN_SEARCH_LIMIT, Math.min(limit, MAX_SEARCH_LIMIT));
        }

        // -------------------------------------------------------------------------
        // Collection Statistics
        // -------------------------------------------------------------------------

        public Map<String, Object> getCollectionInfo() {
                String url = config.getQdrantUrl() + "/collections/" + COLLECTION_NAME;
                HttpHeaders headers = createHeaders();
                HttpEntity<Void> request = new HttpEntity<>(headers);

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                                url, HttpMethod.GET, request,
                                (Class<Map<String, Object>>) (Class<?>) Map.class);

                return response.getBody();
        }
}