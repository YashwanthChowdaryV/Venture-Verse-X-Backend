package com.ventureverse.ventureverse_api.rag.retrieval.rerank;

import com.ventureverse.ventureverse_api.rag.retrieval.hybrid.HybridRetriever.ScoredDocument;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CrossEncoderReranker {

    private static final double FRESHNESS_WEIGHT = 0.15;
    private static final double AUTHORITY_WEIGHT = 0.10;
    private static final double DIVERSITY_THRESHOLD = 0.85;

    @Data
    @Builder
    public static class RerankedResult {
        private List<ScoredDocument> documents;
        private Map<String, Double> relevanceScores;
        private double averageConfidence;
    }

    /**
     * Re-rank documents using cross-encoder scoring with diversity
     */
    public RerankedResult rerank(String query, List<ScoredDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return RerankedResult.builder()
                    .documents(Collections.emptyList())
                    .relevanceScores(Collections.emptyMap())
                    .averageConfidence(0.0)
                    .build();
        }

        // 1. Calculate semantic relevance scores
        Map<String, Double> relevanceScores = calculateRelevanceScores(query, documents);

        // 2. Apply freshness boost
        Map<String, Double> freshnessScores = calculateFreshnessScores(documents);

        // 3. Apply authority boost
        Map<String, Double> authorityScores = calculateAuthorityScores(documents);

        // 4. Combine scores with weights
        List<ScoredDocument> reranked = new ArrayList<>();
        for (ScoredDocument doc : documents) {
            double relevance = relevanceScores.getOrDefault(doc.getId(), 0.0);
            double freshness = freshnessScores.getOrDefault(doc.getId(), 0.5);
            double authority = authorityScores.getOrDefault(doc.getId(), 0.5);

            double finalScore = (relevance * 0.75)
                    + (freshness * FRESHNESS_WEIGHT)
                    + (authority * AUTHORITY_WEIGHT);

            doc.setScore(finalScore);
            reranked.add(doc);
        }

        // 5. Apply diversity - penalize similar documents
        List<ScoredDocument> diverseResults = applyDiversity(reranked);

        // 6. Sort by final score
        diverseResults.sort(Comparator.comparingDouble(ScoredDocument::getScore).reversed());

        double avgConfidence = diverseResults.stream()
                .mapToDouble(ScoredDocument::getScore)
                .average()
                .orElse(0.0);

        return RerankedResult.builder()
                .documents(diverseResults)
                .relevanceScores(relevanceScores)
                .averageConfidence(avgConfidence)
                .build();
    }

    /**
     * Calculate semantic relevance using lexical overlap and position bias
     */
    private Map<String, Double> calculateRelevanceScores(String query, List<ScoredDocument> documents) {
        Map<String, Double> scores = new LinkedHashMap<>();
        String[] queryTerms = query.toLowerCase().split("\\s+");

        for (int i = 0; i < documents.size(); i++) {
            ScoredDocument doc = documents.get(i);
            String content = doc.getContent() != null ? doc.getContent().toLowerCase() : "";
            String title = doc.getTitle() != null ? doc.getTitle().toLowerCase() : "";

            // Term frequency scoring
            double termScore = 0;
            for (String term : queryTerms) {
                if (term.length() > 2) {
                    int contentCount = countOccurrences(content, term);
                    int titleCount = countOccurrences(title, term);
                    termScore += contentCount * 0.5 + titleCount * 1.5;
                }
            }

            // Position bias - earlier results get slight boost
            double positionBias = 1.0 - (i * 0.02);

            // Exact phrase match bonus
            double phraseBonus = content.contains(query.toLowerCase()) ? 0.3 : 0.0;

            scores.put(doc.getId(), (termScore * positionBias) + phraseBonus);
        }

        // Normalize scores to 0-1 range
        double maxScore = scores.values().stream().max(Double::compareTo).orElse(1.0);
        if (maxScore > 0) {
            scores.replaceAll((id, score) -> score / maxScore);
        }

        return scores;
    }

    /**
     * Calculate freshness scores based on document metadata
     */
    private Map<String, Double> calculateFreshnessScores(List<ScoredDocument> documents) {
        Map<String, Double> scores = new LinkedHashMap<>();

        for (ScoredDocument doc : documents) {
            double freshness = 0.7; // Default moderate freshness

            if (doc.getMetadata() != null) {
                Object dateObj = doc.getMetadata().get("publication_date");
                Object freshnessObj = doc.getMetadata().get("freshness_score");

                if (freshnessObj instanceof Number) {
                    freshness = ((Number) freshnessObj).doubleValue();
                }
            }
            scores.put(doc.getId(), freshness);
        }

        return scores;
    }

    /**
     * Calculate authority scores based on source credibility
     */
    private Map<String, Double> calculateAuthorityScores(List<ScoredDocument> documents) {
        Map<String, Double> scores = new LinkedHashMap<>();

        for (ScoredDocument doc : documents) {
            double authority = 0.6; // Default moderate authority

            if (doc.getMetadata() != null) {
                Object authorityObj = doc.getMetadata().get("authority_score");
                if (authorityObj instanceof Number) {
                    authority = ((Number) authorityObj).doubleValue();
                }
            }
            scores.put(doc.getId(), authority);
        }

        return scores;
    }

    /**
     * Apply Maximum Marginal Relevance (MMR) for diversity
     */

    private List<ScoredDocument> applyDiversity(List<ScoredDocument> documents) {
        if (documents.size() <= 3)
            return new ArrayList<>(documents);

        List<ScoredDocument> diverse = new ArrayList<>();
        List<ScoredDocument> remaining = new ArrayList<>(documents);

        // Always keep top result
        diverse.add(remaining.remove(0));

        while (!remaining.isEmpty() && diverse.size() < 10) {
            ScoredDocument best = null;
            double bestMMR = Double.NEGATIVE_INFINITY;

            for (ScoredDocument candidate : remaining) {
                double relevance = candidate.getScore();
                double maxSimilarity = diverse.stream()
                        .mapToDouble(d -> calculateSimilarity(d, candidate))
                        .max()
                        .orElse(0.0);

                // MMR = relevance - lambda * maxSimilarity
                double mmr = relevance - (0.3 * maxSimilarity);

                if (mmr > bestMMR) {
                    bestMMR = mmr;
                    best = candidate;
                }
            }

            if (best != null) {
                remaining.remove(best);
                // Penalize if too similar to existing
                if (bestMMR < best.getScore() * 0.5) {
                    best.setScore(best.getScore() * 0.8);
                }
                diverse.add(best);
            } else {
                break;
            }
        }

        // Add remaining documents
        diverse.addAll(remaining);

        return diverse;
    }

    /**
     * Calculate Jaccard similarity between two documents
     */
    private double calculateSimilarity(ScoredDocument doc1, ScoredDocument doc2) {
        String content1 = doc1.getContent() != null ? doc1.getContent() : "";
        String content2 = doc2.getContent() != null ? doc2.getContent() : "";

        Set<String> words1 = new HashSet<>(Arrays.asList(content1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(content2.toLowerCase().split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private int countOccurrences(String text, String term) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(term, index)) != -1) {
            count++;
            index += term.length();
        }
        return count;
    }
}