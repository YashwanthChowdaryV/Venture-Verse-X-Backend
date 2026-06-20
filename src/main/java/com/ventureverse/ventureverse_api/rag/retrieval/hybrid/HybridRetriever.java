package com.ventureverse.ventureverse_api.rag.retrieval.hybrid;

import com.ventureverse.ventureverse_api.rag.intent.IntentClassifier;
import com.ventureverse.ventureverse_api.rag.intent.IntentClassifier.ClassifiedIntent;
import com.ventureverse.ventureverse_api.rag.service.EmbeddingService;
import com.ventureverse.ventureverse_api.rag.service.impl.QdrantRestServiceImpl;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HybridRetriever {

    private final QdrantRestServiceImpl qdrantService;
    private final EmbeddingService embeddingService;

    private static final double RRF_CONSTANT = 60.0;
    private static final int TOP_K_DENSE = 20;
    private static final int TOP_K_KEYWORD = 15;

    @Data
    @Builder
    public static class ScoredDocument {
        private String id;
        private String content;
        private String title;
        private double score;
        private double denseScore;
        private double keywordScore;
        private String source;
        private Map<String, Object> metadata;
    }

    @Data
    @Builder
    public static class HybridResult {
        private List<ScoredDocument> documents;
        private long retrievalTimeMs;
        private int totalCandidates;
    }

    public HybridResult search(String query) {
        long startTime = System.currentTimeMillis();

        CompletableFuture<List<ScoredDocument>> denseFuture = CompletableFuture
                .supplyAsync(() -> denseRetrieval(query));
        CompletableFuture<List<ScoredDocument>> keywordFuture = CompletableFuture
                .supplyAsync(() -> keywordRetrieval(query));

        List<ScoredDocument> denseResults = denseFuture.join();
        List<ScoredDocument> keywordResults = keywordFuture.join();

        Map<String, ScoredDocument> fusedResults = reciprocalRankFusion(denseResults, keywordResults);

        List<ScoredDocument> finalResults = fusedResults.values().stream()
                .sorted(Comparator.comparingDouble(ScoredDocument::getScore).reversed())
                .limit(10)
                .collect(Collectors.toList());

        long elapsed = System.currentTimeMillis() - startTime;

        return HybridResult.builder()
                .documents(finalResults)
                .retrievalTimeMs(elapsed)
                .totalCandidates(denseResults.size() + keywordResults.size())
                .build();
    }

    private List<ScoredDocument> denseRetrieval(String query) {
        try {
            List<Float> queryVector = embeddingService.createEmbedding(query);
            List<Map<String, Object>> rawResults = qdrantService.searchWithScores(queryVector, TOP_K_DENSE);
            return parseDenseResults(rawResults);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<ScoredDocument> keywordRetrieval(String query) {
        try {
            List<Float> queryVector = embeddingService.createEmbedding(query);
            List<Map<String, Object>> rawResults = qdrantService.searchWithKeywordBoost(
                    queryVector, extractKeywords(query), TOP_K_KEYWORD);
            return parseKeywordResults(rawResults);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<String, ScoredDocument> reciprocalRankFusion(
            List<ScoredDocument> denseResults,
            List<ScoredDocument> keywordResults) {

        Map<String, ScoredDocument> fused = new LinkedHashMap<>();

        for (int i = 0; i < denseResults.size(); i++) {
            ScoredDocument doc = denseResults.get(i);
            double rrfScore = 1.0 / (RRF_CONSTANT + i + 1);
            doc.setDenseScore(rrfScore);
            doc.setScore(rrfScore);
            fused.put(doc.getId(), doc);
        }

        for (int i = 0; i < keywordResults.size(); i++) {
            ScoredDocument doc = keywordResults.get(i);
            double rrfScore = 1.0 / (RRF_CONSTANT + i + 1);
            if (fused.containsKey(doc.getId())) {
                ScoredDocument existing = fused.get(doc.getId());
                existing.setKeywordScore(rrfScore);
                existing.setScore(existing.getScore() + rrfScore);
            } else {
                doc.setKeywordScore(rrfScore);
                doc.setScore(rrfScore);
                fused.put(doc.getId(), doc);
            }
        }
        return fused;
    }

    private List<String> extractKeywords(String query) {
        Set<String> stopWords = Set.of(
                "the", "is", "at", "which", "on", "a", "an", "and", "or", "but",
                "in", "with", "to", "for", "of", "from", "by", "what", "how",
                "when", "where", "who", "why", "can", "does", "do", "are", "was");
        return Arrays.stream(query.toLowerCase().split("\\s+"))
                .filter(word -> word.length() > 2)
                .filter(word -> !stopWords.contains(word))
                .distinct()
                .collect(Collectors.toList());
    }

    private List<ScoredDocument> parseDenseResults(List<Map<String, Object>> rawResults) {
        List<ScoredDocument> documents = new ArrayList<>();
        for (int i = 0; i < rawResults.size(); i++) {
            Map<String, Object> result = rawResults.get(i);
            Map<?, ?> payload = (Map<?, ?>) result.get("payload");
            documents.add(ScoredDocument.builder()
                    .id(result.get("id").toString())
                    .content(payload.get("content") != null ? payload.get("content").toString() : "")
                    .title(payload.get("title") != null ? payload.get("title").toString() : "")
                    .source("vector_search")
                    .score(1.0 - (i * 0.05))
                    .build());
        }
        return documents;
    }

    private final IntentClassifier intentClassifier;

    // Add this constructor injection if not using @RequiredArgsConstructor for all
    // fields
    // Or update the existing @RequiredArgsConstructor

    public HybridResult searchWithIntent(String query) {
        IntentClassifier.ClassifiedIntent intent = intentClassifier.classify(query);

        long startTime = System.currentTimeMillis();

        CompletableFuture<List<ScoredDocument>> denseFuture = CompletableFuture
                .supplyAsync(() -> denseRetrieval(query));
        CompletableFuture<List<ScoredDocument>> keywordFuture = CompletableFuture
                .supplyAsync(() -> keywordRetrievalWithIntent(query, intent));

        List<ScoredDocument> denseResults = denseFuture.join();
        List<ScoredDocument> keywordResults = keywordFuture.join();

        Map<String, ScoredDocument> fusedResults = reciprocalRankFusion(denseResults, keywordResults);

        List<ScoredDocument> finalResults = fusedResults.values().stream()
                .sorted(Comparator.comparingDouble(ScoredDocument::getScore).reversed())
                .limit(10)
                .collect(Collectors.toList());

        long elapsed = System.currentTimeMillis() - startTime;

        return HybridResult.builder()
                .documents(finalResults)
                .retrievalTimeMs(elapsed)
                .totalCandidates(denseResults.size() + keywordResults.size())
                .build();
    }

    private List<ScoredDocument> keywordRetrievalWithIntent(String query, IntentClassifier.ClassifiedIntent intent) {
        try {
            List<Float> queryVector = embeddingService.createEmbedding(query);
            List<String> enhancedKeywords = new ArrayList<>(extractKeywords(query));
            enhancedKeywords.addAll(intent.getExtractedEntities());

            List<Map<String, Object>> rawResults = qdrantService.searchWithKeywordBoost(
                    queryVector, enhancedKeywords, TOP_K_KEYWORD);
            return parseKeywordResults(rawResults);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<ScoredDocument> parseKeywordResults(List<Map<String, Object>> rawResults) {
        List<ScoredDocument> documents = new ArrayList<>();
        for (int i = 0; i < rawResults.size(); i++) {
            Map<String, Object> result = rawResults.get(i);
            Map<?, ?> payload = (Map<?, ?>) result.get("payload");
            documents.add(ScoredDocument.builder()
                    .id(result.get("id").toString())
                    .content(payload.get("content") != null ? payload.get("content").toString() : "")
                    .title(payload.get("title") != null ? payload.get("title").toString() : "")
                    .source("keyword_match")
                    .score(0.9 - (i * 0.06))
                    .build());
        }
        return documents;
    }
}