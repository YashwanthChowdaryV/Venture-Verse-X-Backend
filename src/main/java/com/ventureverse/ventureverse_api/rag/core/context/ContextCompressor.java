package com.ventureverse.ventureverse_api.rag.core.context;

import com.ventureverse.ventureverse_api.rag.retrieval.hybrid.HybridRetriever.ScoredDocument;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ContextCompressor {

    private static final int MAX_TOKENS = 3000;
    private static final int CHARS_PER_TOKEN = 4;
    private static final double REDUNDANCY_THRESHOLD = 0.7;

    @Data
    @Builder
    public static class CompressedContext {
        private String compressedText;
        private List<String> sourceIds;
        private int originalTokens;
        private int compressedTokens;
        private double compressionRatio;
        private Map<String, Double> sourceWeights;
    }

    /**
     * Compress retrieved documents to fit within token budget while preserving key
     * information
     */
    public CompressedContext compress(String query, List<ScoredDocument> documents) {
        if (documents == null || documents.isEmpty()) {
            return CompressedContext.builder()
                    .compressedText("")
                    .sourceIds(Collections.emptyList())
                    .originalTokens(0)
                    .compressedTokens(0)
                    .compressionRatio(1.0)
                    .sourceWeights(Collections.emptyMap())
                    .build();
        }

        int originalTokens = estimateTotalTokens(documents);
        Map<String, Double> sourceWeights = calculateSourceWeights(documents);

        // Stage 1: Extract key sentences using query relevance
        List<WeightedSentence> sentences = extractWeightedSentences(query, documents);

        // Stage 2: Remove redundant sentences
        sentences = removeRedundancy(sentences);

        // Stage 3: Rank by importance
        sentences.sort(Comparator.comparingDouble(WeightedSentence::getScore).reversed());

        // Stage 4: Fit within token budget
        List<WeightedSentence> selected = fitTokenBudget(sentences, MAX_TOKENS);

        // Stage 5: Order by original document sequence for readability
        selected.sort(Comparator.comparingInt(WeightedSentence::getOriginalPosition));

        // Stage 6: Build compressed text
        String compressedText = selected.stream()
                .map(WeightedSentence::getText)
                .collect(Collectors.joining(" "));

        List<String> sourceIds = selected.stream()
                .map(WeightedSentence::getSourceId)
                .distinct()
                .collect(Collectors.toList());

        int compressedTokens = estimateTokens(compressedText);

        return CompressedContext.builder()
                .compressedText(compressedText)
                .sourceIds(sourceIds)
                .originalTokens(originalTokens)
                .compressedTokens(compressedTokens)
                .compressionRatio((double) compressedTokens / originalTokens)
                .sourceWeights(sourceWeights)
                .build();
    }

    /**
     * Extract sentences weighted by query relevance and source importance
     */
    private List<WeightedSentence> extractWeightedSentences(String query, List<ScoredDocument> documents) {
        List<WeightedSentence> sentences = new ArrayList<>();
        String[] queryTerms = query.toLowerCase().split("\\s+");
        int globalPosition = 0;

        for (ScoredDocument doc : documents) {
            if (doc.getContent() == null)
                continue;

            String[] docSentences = doc.getContent().split("(?<=[.!?])\\s+");

            for (int i = 0; i < docSentences.length; i++) {
                String sentence = docSentences[i].trim();
                if (sentence.length() < 20)
                    continue;

                double termScore = 0;
                String lowerSentence = sentence.toLowerCase();

                for (String term : queryTerms) {
                    if (term.length() > 2 && lowerSentence.contains(term)) {
                        termScore += 1.0;
                    }
                }

                // Weight by document score, sentence position, and term match
                double positionWeight = 1.0 - (i * 0.02); // Earlier sentences weight more
                double score = (termScore * 2.0 + doc.getScore() * 3.0) * positionWeight;

                sentences.add(WeightedSentence.builder()
                        .text(sentence)
                        .score(score)
                        .sourceId(doc.getId())
                        .sourceTitle(doc.getTitle())
                        .originalPosition(globalPosition)
                        .sourceScore(doc.getScore())
                        .build());

                globalPosition++;
            }
        }
        return sentences;
    }

    /**
     * Remove sentences that are too similar to higher-ranked sentences
     */
    private List<WeightedSentence> removeRedundancy(List<WeightedSentence> sentences) {
        if (sentences.size() <= 1)
            return sentences;

        sentences.sort(Comparator.comparingDouble(WeightedSentence::getScore).reversed());
        List<WeightedSentence> unique = new ArrayList<>();
        unique.add(sentences.get(0));

        for (int i = 1; i < sentences.size(); i++) {
            boolean isRedundant = false;
            for (WeightedSentence existing : unique) {
                double similarity = calculateJaccardSimilarity(
                        sentences.get(i).getText(), existing.getText());
                if (similarity > REDUNDANCY_THRESHOLD) {
                    isRedundant = true;
                    break;
                }
            }
            if (!isRedundant) {
                unique.add(sentences.get(i));
            }
        }
        return unique;
    }

    /**
     * Select top sentences that fit within token budget
     */
    private List<WeightedSentence> fitTokenBudget(List<WeightedSentence> sentences, int maxTokens) {
        List<WeightedSentence> selected = new ArrayList<>();
        int tokenCount = 0;
        int maxChars = maxTokens * CHARS_PER_TOKEN;

        for (WeightedSentence sentence : sentences) {
            int sentenceTokens = estimateTokens(sentence.getText());
            if (tokenCount + sentenceTokens <= maxTokens) {
                selected.add(sentence);
                tokenCount += sentenceTokens;
            }
        }
        return selected;
    }

    /**
     * Calculate weight of each source in the compressed context
     */
    private Map<String, Double> calculateSourceWeights(List<ScoredDocument> documents) {
        Map<String, Double> weights = new LinkedHashMap<>();
        double totalScore = documents.stream().mapToDouble(ScoredDocument::getScore).sum();

        for (ScoredDocument doc : documents) {
            weights.put(doc.getId(), totalScore > 0 ? doc.getScore() / totalScore : 0);
        }
        return weights;
    }

    private double calculateJaccardSimilarity(String text1, String text2) {
        Set<String> words1 = new HashSet<>(Arrays.asList(text1.toLowerCase().split("\\s+")));
        Set<String> words2 = new HashSet<>(Arrays.asList(text2.toLowerCase().split("\\s+")));

        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);

        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private int estimateTokens(String text) {
        return (int) Math.ceil(text.length() / (double) CHARS_PER_TOKEN);
    }

    private int estimateTotalTokens(List<ScoredDocument> documents) {
        return documents.stream()
                .filter(doc -> doc.getContent() != null)
                .mapToInt(doc -> estimateTokens(doc.getContent()))
                .sum();
    }

    @Data
    @Builder
    private static class WeightedSentence {
        private String text;
        private double score;
        private String sourceId;
        private String sourceTitle;
        private int originalPosition;
        private double sourceScore;
    }
}