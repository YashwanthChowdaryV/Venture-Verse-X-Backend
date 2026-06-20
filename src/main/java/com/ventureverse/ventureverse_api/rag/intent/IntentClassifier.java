package com.ventureverse.ventureverse_api.rag.intent;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class IntentClassifier {

    private static final Map<IntentType, List<String>> INTENT_PATTERNS = new LinkedHashMap<>();

    static {
        INTENT_PATTERNS.put(IntentType.DEFINITION, List.of(
                "what is", "define", "explain", "meaning of", "what are",
                "what does", "definition", "describe", "tell me about"));
        INTENT_PATTERNS.put(IntentType.HOW_TO, List.of(
                "how to", "how do i", "how can", "steps to", "process of",
                "way to", "method", "approach", "guide"));
        INTENT_PATTERNS.put(IntentType.COMPARISON, List.of(
                "vs", "versus", "compare", "difference between", "better",
                "or", "which is", "pros and cons"));
        INTENT_PATTERNS.put(IntentType.CALCULATION, List.of(
                "calculate", "how much", "formula", "compute", "metrics",
                "measure", "estimate", "rate of"));
        INTENT_PATTERNS.put(IntentType.FUNDING_ADVICE, List.of(
                "funding", "raise", "series a", "series b", "seed",
                "investor", "vc", "venture capital", "valuation", "term sheet"));
        INTENT_PATTERNS.put(IntentType.MARKET_ANALYSIS, List.of(
                "market", "tam", "sam", "som", "industry", "sector",
                "landscape", "competitor", "size", "opportunity"));
        INTENT_PATTERNS.put(IntentType.RISK_ASSESSMENT, List.of(
                "risk", "failure", "pitfall", "challenge", "problem",
                "threat", "downside", "danger", "avoid"));
        INTENT_PATTERNS.put(IntentType.STRATEGY, List.of(
                "strategy", "plan", "gtm", "go to market", "growth",
                "scale", "acquisition", "retention", "marketing"));
    }

    @Data
    @Builder
    public static class ClassifiedIntent {
        private IntentType primaryIntent;
        private Map<IntentType, Double> confidenceScores;
        private List<String> extractedEntities;
        private String cleanedQuery;
    }

    public ClassifiedIntent classify(String query) {
        String lowerQuery = query.toLowerCase().trim();
        Map<IntentType, Double> scores = new LinkedHashMap<>();

        for (Map.Entry<IntentType, List<String>> entry : INTENT_PATTERNS.entrySet()) {
            double score = 0;
            for (String pattern : entry.getValue()) {
                if (lowerQuery.contains(pattern)) {
                    score += 1.0;
                }
            }
            if (score > 0) {
                scores.put(entry.getKey(), score / entry.getValue().size());
            }
        }

        IntentType primary = scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(IntentType.GENERAL);

        return ClassifiedIntent.builder()
                .primaryIntent(primary)
                .confidenceScores(scores)
                .extractedEntities(extractEntities(lowerQuery))
                .cleanedQuery(lowerQuery)
                .build();
    }

    private List<String> extractEntities(String query) {
        List<String> entities = new ArrayList<>();
        String[] words = query.split("\\s+");

        Set<String> startupEntities = Set.of(
                "tam", "sam", "som", "cac", "ltv", "arr", "mrr",
                "saas", "b2b", "b2c", "mvp", "pmf", "gtm", "vc");

        for (String word : words) {
            if (startupEntities.contains(word)) {
                entities.add(word.toUpperCase());
            }
        }
        return entities;
    }
}