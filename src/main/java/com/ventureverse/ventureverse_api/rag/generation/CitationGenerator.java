package com.ventureverse.ventureverse_api.rag.generation;

import com.ventureverse.ventureverse_api.rag.retrieval.hybrid.HybridRetriever.ScoredDocument;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CitationGenerator {

    private static final double MIN_CONTRIBUTION_THRESHOLD = 0.2;
    private static final int MAX_SNIPPET_LENGTH = 300;

    @Data
    @Builder
    public static class Citation {
        private String id;
        private int number;
        private String snippet;
        private String sourceTitle;
        private String sourceUrl;
        private String sourceType;
        private double confidence;
        private String chunkId;
        private boolean isPrimary;
    }

    @Data
    @Builder
    public static class CitedAnswer {
        private String answer;
        private List<Citation> citations;
        private Map<String, Double> sourceContributions;
        private double overallConfidence;
    }

    /**
     * Generate citations for an answer based on source documents
     */
    public CitedAnswer generateCitations(String answer, List<ScoredDocument> sources) {
        List<Citation> citations = new ArrayList<>();
        Map<String, Double> sourceContributions = new LinkedHashMap<>();

        // 1. Calculate contribution of each source
        for (ScoredDocument source : sources) {
            double contribution = calculateContribution(answer, source);
            sourceContributions.put(source.getId(), contribution);

            // 2. Create citation if contribution exceeds threshold
            if (contribution >= MIN_CONTRIBUTION_THRESHOLD) {
                Citation citation = Citation.builder()
                        .id("cite_" + source.getId())
                        .number(citations.size() + 1)
                        .snippet(extractRelevantSnippet(answer, source))
                        .sourceTitle(source.getTitle() != null ? source.getTitle() : "Unknown Source")
                        .sourceUrl(extractSourceUrl(source))
                        .sourceType(source.getSource() != null ? source.getSource() : "document")
                        .confidence(contribution)
                        .chunkId(source.getId())
                        .isPrimary(contribution >= 0.5)
                        .build();

                citations.add(citation);
            }
        }

        // 3. Sort citations by confidence
        citations.sort(Comparator.comparingDouble(Citation::getConfidence).reversed());

        // 4. Renumber after sorting
        for (int i = 0; i < citations.size(); i++) {
            citations.get(i).setNumber(i + 1);
        }

        // 5. Calculate overall confidence
        double overallConfidence = calculateOverallConfidence(citations, sources);

        return CitedAnswer.builder()
                .answer(answer)
                .citations(citations)
                .sourceContributions(sourceContributions)
                .overallConfidence(overallConfidence)
                .build();
    }

    /**
     * Annotate answer text with citation markers [1], [2], etc.
     */
    public String annotateAnswerWithCitations(String answer, List<Citation> citations) {
        if (citations.isEmpty())
            return answer;

        StringBuilder annotated = new StringBuilder(answer);
        List<int[]> insertions = new ArrayList<>();

        // Find where to insert citation markers
        for (Citation citation : citations) {
            String snippet = citation.getSnippet();
            if (snippet != null && !snippet.isEmpty()) {
                int position = findBestInsertionPoint(answer, snippet);
                if (position >= 0) {
                    insertions.add(new int[] { position, citation.getNumber() });
                }
            }
        }

        // Insert markers from end to start to preserve positions
        insertions.sort((a, b) -> Integer.compare(b[0], a[0]));
        for (int[] insertion : insertions) {
            String marker = " [" + insertion[1] + "]";
            annotated.insert(insertion[0], marker);
        }

        return annotated.toString();
    }

    /**
     * Calculate how much a source contributed to the answer
     */
    private double calculateContribution(String answer, ScoredDocument source) {
        if (source.getContent() == null || source.getContent().isEmpty())
            return 0.0;

        String content = source.getContent().toLowerCase();
        String answerLower = answer.toLowerCase();

        // 1. Extract key sentences from source
        List<String> sourceSentences = extractSentences(content);

        // 2. Calculate n-gram overlap
        double overlapScore = calculateNgramOverlap(answerLower, content);

        // 3. Check if key phrases from source appear in answer
        double phraseScore = calculatePhraseMatch(answerLower, sourceSentences);

        // 4. Weight by source's retrieval score
        double retrievalWeight = source.getScore();

        return (overlapScore * 0.4 + phraseScore * 0.3 + retrievalWeight * 0.3);
    }

    /**
     * Extract the most relevant snippet from source for the answer
     */
    private String extractRelevantSnippet(String answer, ScoredDocument source) {
        if (source.getContent() == null || source.getContent().isEmpty())
            return "";

        String content = source.getContent();
        List<String> sentences = extractSentences(content);

        // Find sentence with highest overlap with answer
        String bestSentence = "";
        double bestScore = 0;

        for (String sentence : sentences) {
            double score = calculateNgramOverlap(
                    answer.toLowerCase(),
                    sentence.toLowerCase());
            if (score > bestScore) {
                bestScore = score;
                bestSentence = sentence;
            }
        }

        // Truncate if too long
        if (bestSentence.length() > MAX_SNIPPET_LENGTH) {
            bestSentence = bestSentence.substring(0, MAX_SNIPPET_LENGTH) + "...";
        }

        return bestSentence.trim();
    }

    /**
     * Calculate n-gram overlap between two texts
     */
    private double calculateNgramOverlap(String text1, String text2) {
        Set<String> ngrams1 = generateNgrams(text1, 3);
        Set<String> ngrams2 = generateNgrams(text2, 3);

        Set<String> intersection = new HashSet<>(ngrams1);
        intersection.retainAll(ngrams2);

        Set<String> union = new HashSet<>(ngrams1);
        union.addAll(ngrams2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * Calculate phrase-level matches
     */
    private double calculatePhraseMatch(String answer, List<String> sourceSentences) {
        int matches = 0;
        String[] answerWords = answer.split("\\s+");

        for (String sentence : sourceSentences) {
            for (int i = 0; i < answerWords.length - 2; i++) {
                String phrase = String.join(" ",
                        Arrays.copyOfRange(answerWords, i, Math.min(i + 3, answerWords.length)));
                if (sentence.contains(phrase)) {
                    matches++;
                }
            }
        }

        return Math.min(1.0, matches / 10.0);
    }

    /**
     * Find the best position to insert a citation marker
     */
    private int findBestInsertionPoint(String answer, String snippet) {
        if (snippet == null || snippet.isEmpty())
            return -1;

        String[] snippetWords = snippet.split("\\s+");
        if (snippetWords.length < 3)
            return -1;

        // Use first 3 words of snippet to locate position
        String searchPhrase = String.join(" ",
                Arrays.copyOfRange(snippetWords, 0, Math.min(3, snippetWords.length)))
                .toLowerCase();

        int position = answer.toLowerCase().indexOf(searchPhrase);
        if (position >= 0) {
            return position + searchPhrase.length();
        }

        // Fallback: end of first sentence
        int firstPeriod = answer.indexOf('.');
        return firstPeriod >= 0 ? firstPeriod + 1 : answer.length();
    }

    /**
     * Extract source URL from document metadata
     */
    private String extractSourceUrl(ScoredDocument source) {
        if (source.getMetadata() != null) {
            Object url = source.getMetadata().get("source_url");
            if (url != null)
                return url.toString();
            url = source.getMetadata().get("url");
            if (url != null)
                return url.toString();
        }
        return "";
    }

    /**
     * Calculate overall confidence score
     */
    private double calculateOverallConfidence(List<Citation> citations, List<ScoredDocument> sources) {
        if (citations.isEmpty())
            return 0.0;

        // Average of top citation confidences
        double citationConfidence = citations.stream()
                .limit(5)
                .mapToDouble(Citation::getConfidence)
                .average()
                .orElse(0.0);

        // Source diversity bonus
        long uniqueSources = citations.stream()
                .map(Citation::getSourceTitle)
                .distinct()
                .count();
        double diversityBonus = Math.min(0.2, uniqueSources * 0.05);

        return Math.min(1.0, citationConfidence + diversityBonus);
    }

    private List<String> extractSentences(String text) {
        return Arrays.stream(text.split("(?<=[.!?])\\s+"))
                .map(String::trim)
                .filter(s -> s.length() > 10)
                .collect(Collectors.toList());
    }

    private Set<String> generateNgrams(String text, int n) {
        Set<String> ngrams = new HashSet<>();
        String[] words = text.split("\\s+");
        for (int i = 0; i <= words.length - n; i++) {
            ngrams.add(String.join(" ", Arrays.copyOfRange(words, i, i + n)));
        }
        return ngrams;
    }
}