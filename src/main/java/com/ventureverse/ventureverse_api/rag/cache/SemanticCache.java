package com.ventureverse.ventureverse_api.rag.cache;

import com.ventureverse.ventureverse_api.rag.generation.AnswerGenerator.GeneratedAnswer;
import com.ventureverse.ventureverse_api.rag.retrieval.hybrid.HybridRetriever.HybridResult;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SemanticCache {

        private static final int MAX_CACHE_SIZE = 500;
        private static final long EXACT_TTL_MS = 300_000; // 5 minutes exact match
        private static final long SEMANTIC_TTL_MS = 900_000; // 15 minutes semantic match
        private static final double SIMILARITY_THRESHOLD = 0.85;

        private final Map<String, CacheEntry<HybridResult>> retrievalCache = new ConcurrentHashMap<>();
        private final Map<String, CacheEntry<GeneratedAnswer>> answerCache = new ConcurrentHashMap<>();
        private final Map<String, List<Float>> embeddingCache = new ConcurrentHashMap<>();

        @Data
        @Builder
        private static class CacheEntry<T> {
                private T data;
                private long timestamp;
                private String query;
                private int hitCount;
        }

        /**
         * Get cached retrieval result if available
         */
        public Optional<HybridResult> getRetrieval(String query) {
                String normalizedQuery = normalizeQuery(query);
                CacheEntry<HybridResult> entry = retrievalCache.get(normalizedQuery);

                if (entry != null && !isExpired(entry, EXACT_TTL_MS)) {
                        entry.setHitCount(entry.getHitCount() + 1);
                        return Optional.of(entry.getData());
                }

                // Try semantic match
                Optional<CacheEntry<HybridResult>> semanticMatch = findSemanticMatch(
                                retrievalCache, query, SEMANTIC_TTL_MS);

                if (semanticMatch.isPresent()) {
                        semanticMatch.get().setHitCount(semanticMatch.get().getHitCount() + 1);
                        return Optional.of(semanticMatch.get().getData());
                }

                return Optional.empty();
        }

        /**
         * Cache retrieval result
         */
        public void putRetrieval(String query, HybridResult result) {
                String normalizedQuery = normalizeQuery(query);
                retrievalCache.put(normalizedQuery, CacheEntry.<HybridResult>builder()
                                .data(result)
                                .timestamp(System.currentTimeMillis())
                                .query(query)
                                .hitCount(1)
                                .build());

                evictIfNeeded(retrievalCache);
        }

        /**
         * Get cached answer if available
         */
        public Optional<GeneratedAnswer> getAnswer(String query) {
                String normalizedQuery = normalizeQuery(query);
                CacheEntry<GeneratedAnswer> entry = answerCache.get(normalizedQuery);

                if (entry != null && !isExpired(entry, EXACT_TTL_MS)) {
                        entry.setHitCount(entry.getHitCount() + 1);
                        return Optional.of(entry.getData());
                }

                Optional<CacheEntry<GeneratedAnswer>> semanticMatch = findSemanticMatch(
                                answerCache, query, SEMANTIC_TTL_MS);

                if (semanticMatch.isPresent()) {
                        semanticMatch.get().setHitCount(semanticMatch.get().getHitCount() + 1);
                        return Optional.of(semanticMatch.get().getData());
                }

                return Optional.empty();
        }

        /**
         * Cache generated answer
         */
        public void putAnswer(String query, GeneratedAnswer answer) {
                String normalizedQuery = normalizeQuery(query);
                answerCache.put(normalizedQuery, CacheEntry.<GeneratedAnswer>builder()
                                .data(answer)
                                .timestamp(System.currentTimeMillis())
                                .query(query)
                                .hitCount(1)
                                .build());

                evictIfNeeded(answerCache);
        }

        /**
         * Cache embedding vector to avoid re-computing
         */
        public Optional<List<Float>> getEmbedding(String text) {
                String key = normalizeQuery(text);
                return Optional.ofNullable(embeddingCache.get(key));
        }

        public void putEmbedding(String text, List<Float> embedding) {
                String key = normalizeQuery(text);
                embeddingCache.put(key, embedding);

                if (embeddingCache.size() > MAX_CACHE_SIZE * 2) {
                        embeddingCache.clear();
                }
        }

        /**
         * Find semantically similar cached query
         */
        private <T> Optional<CacheEntry<T>> findSemanticMatch(
                        Map<String, CacheEntry<T>> cache, String query, long ttl) {

                String normalizedQuery = normalizeQuery(query);

                return cache.entrySet().stream()
                                .filter(e -> !isExpired(e.getValue(), ttl))
                                .filter(e -> calculateSimilarity(normalizedQuery, e.getKey()) > SIMILARITY_THRESHOLD)
                                .max(Comparator.comparingDouble(e -> calculateSimilarity(normalizedQuery, e.getKey())))
                                .map(Map.Entry::getValue);
        }

        /**
         * Simple Jaccard similarity for query matching
         */
        private double calculateSimilarity(String query1, String query2) {
                Set<String> words1 = new HashSet<>(Arrays.asList(query1.split("\\s+")));
                Set<String> words2 = new HashSet<>(Arrays.asList(query2.split("\\s+")));

                Set<String> intersection = new HashSet<>(words1);
                intersection.retainAll(words2);

                Set<String> union = new HashSet<>(words1);
                union.addAll(words2);

                return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
        }

        private String normalizeQuery(String query) {
                return query.toLowerCase().trim().replaceAll("\\s+", " ");
        }

        private <T> boolean isExpired(CacheEntry<T> entry, long ttl) {
                return System.currentTimeMillis() - entry.getTimestamp() > ttl;
        }

        private <T> void evictIfNeeded(Map<String, CacheEntry<T>> cache) {
                if (cache.size() > MAX_CACHE_SIZE) {
                        // Remove oldest 20% of entries
                        int toRemove = (int) (MAX_CACHE_SIZE * 0.2);
                        cache.entrySet().stream()
                                        .sorted(Comparator.comparingLong(e -> e.getValue().getTimestamp()))
                                        .limit(toRemove)
                                        .forEach(e -> cache.remove(e.getKey()));
                }
        }

        /**
         * Cache statistics for monitoring
         */
        public Map<String, Object> getStats() {
                Map<String, Object> stats = new LinkedHashMap<>();
                stats.put("retrievalCacheSize", retrievalCache.size());
                stats.put("answerCacheSize", answerCache.size());
                stats.put("embeddingCacheSize", embeddingCache.size());
                stats.put("retrievalHitRate", calculateHitRate(retrievalCache));
                stats.put("answerHitRate", calculateHitRate(answerCache));
                return stats;
        }

        private <T> double calculateHitRate(Map<String, CacheEntry<T>> cache) {
                if (cache.isEmpty())
                        return 0.0;
                double totalHits = cache.values().stream().mapToInt(CacheEntry::getHitCount).sum();
                return totalHits / (cache.size() + totalHits);
        }

        public void clear() {
                retrievalCache.clear();
                answerCache.clear();
                embeddingCache.clear();
        }
}