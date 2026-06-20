package com.ventureverse.ventureverse_api.rag.observability;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsCollector {

    private final AtomicLong totalSearches = new AtomicLong(0);
    private final AtomicLong totalAnswers = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    private final List<SearchMetric> recentSearches = Collections.synchronizedList(new ArrayList<>());
    private final List<AnswerMetric> recentAnswers = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, AtomicLong> intentDistribution = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> errorCounts = new ConcurrentHashMap<>();

    private static final int MAX_RECENT = 100;

    @Data
    @Builder
    public static class SearchMetric {
        private String query;
        private long retrievalTimeMs;
        private long rerankTimeMs;
        private int candidateCount;
        private int resultCount;
        private boolean fromCache;
        private long timestamp;
    }

    @Data
    @Builder
    public static class AnswerMetric {
        private String query;
        private long retrievalTimeMs;
        private long generationTimeMs;
        private long totalTimeMs;
        private int tokensUsed;
        private double confidence;
        private boolean fromCache;
        private long timestamp;
    }

    @Data
    @Builder
    public static class DashboardMetrics {
        private long totalSearches;
        private long totalAnswers;
        private double cacheHitRate;
        private double avgRetrievalMs;
        private double avgGenerationMs;
        private double avgConfidence;
        private Map<String, Long> intentDistribution;
        private Map<String, Long> errorCounts;
        private List<SearchMetric> recentSearches;
        private List<AnswerMetric> recentAnswers;
    }

    public void recordSearch(String query, long retrievalMs, long rerankMs, int candidates, int results,
            boolean fromCache) {
        totalSearches.incrementAndGet();
        if (fromCache)
            cacheHits.incrementAndGet();
        else
            cacheMisses.incrementAndGet();

        SearchMetric metric = SearchMetric.builder()
                .query(query)
                .retrievalTimeMs(retrievalMs)
                .rerankTimeMs(rerankMs)
                .candidateCount(candidates)
                .resultCount(results)
                .fromCache(fromCache)
                .timestamp(System.currentTimeMillis())
                .build();

        recentSearches.add(0, metric);
        if (recentSearches.size() > MAX_RECENT) {
            recentSearches.remove(recentSearches.size() - 1);
        }
    }

    public void recordAnswer(String query, long retrievalMs, long generationMs, int tokens, double confidence,
            boolean fromCache) {
        totalAnswers.incrementAndGet();

        AnswerMetric metric = AnswerMetric.builder()
                .query(query)
                .retrievalTimeMs(retrievalMs)
                .generationTimeMs(generationMs)
                .totalTimeMs(retrievalMs + generationMs)
                .tokensUsed(tokens)
                .confidence(confidence)
                .fromCache(fromCache)
                .timestamp(System.currentTimeMillis())
                .build();

        recentAnswers.add(0, metric);
        if (recentAnswers.size() > MAX_RECENT) {
            recentAnswers.remove(recentAnswers.size() - 1);
        }
    }

    public void recordIntent(String intent) {
        intentDistribution.computeIfAbsent(intent, k -> new AtomicLong(0)).incrementAndGet();
    }

    public void recordError(String errorType) {
        errorCounts.computeIfAbsent(errorType, k -> new AtomicLong(0)).incrementAndGet();
    }

    public DashboardMetrics getDashboard() {
        Map<String, Long> intents = new LinkedHashMap<>();
        intentDistribution.forEach((k, v) -> intents.put(k, v.get()));

        Map<String, Long> errors = new LinkedHashMap<>();
        errorCounts.forEach((k, v) -> errors.put(k, v.get()));

        return DashboardMetrics.builder()
                .totalSearches(totalSearches.get())
                .totalAnswers(totalAnswers.get())
                .cacheHitRate(calculateHitRate())
                .avgRetrievalMs(calculateAvgRetrievalMs())
                .avgGenerationMs(calculateAvgGenerationMs())
                .avgConfidence(calculateAvgConfidence())
                .intentDistribution(intents)
                .errorCounts(errors)
                .recentSearches(new ArrayList<>(recentSearches.subList(0, Math.min(20, recentSearches.size()))))
                .recentAnswers(new ArrayList<>(recentAnswers.subList(0, Math.min(20, recentAnswers.size()))))
                .build();
    }

    private double calculateHitRate() {
        long total = cacheHits.get() + cacheMisses.get();
        return total > 0 ? (double) cacheHits.get() / total : 0.0;
    }

    private double calculateAvgRetrievalMs() {
        if (recentSearches.isEmpty())
            return 0;
        return recentSearches.stream()
                .mapToLong(SearchMetric::getRetrievalTimeMs)
                .average().orElse(0);
    }

    private double calculateAvgGenerationMs() {
        if (recentAnswers.isEmpty())
            return 0;
        return recentAnswers.stream()
                .mapToLong(AnswerMetric::getGenerationTimeMs)
                .average().orElse(0);
    }

    private double calculateAvgConfidence() {
        if (recentAnswers.isEmpty())
            return 0;
        return recentAnswers.stream()
                .mapToDouble(AnswerMetric::getConfidence)
                .average().orElse(0);
    }

    public void reset() {
        totalSearches.set(0);
        totalAnswers.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        recentSearches.clear();
        recentAnswers.clear();
        intentDistribution.clear();
        errorCounts.clear();
    }
}