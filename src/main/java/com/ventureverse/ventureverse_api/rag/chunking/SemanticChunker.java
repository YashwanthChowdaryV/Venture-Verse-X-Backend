package com.ventureverse.ventureverse_api.rag.chunking;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class SemanticChunker {

    private static final int DEFAULT_CHUNK_SIZE = 500;
    private static final int DEFAULT_OVERLAP = 100;
    private static final Pattern SENTENCE_BREAK = Pattern.compile("(?<=[.!?])\\s+");

    @Data
    @Builder
    public static class Chunk {
        private String id;
        private String text;
        private int startIndex;
        private int endIndex;
        private String section;
        private int chunkNumber;
        private int totalChunks;
    }

    public List<Chunk> chunk(String content) {
        return chunk(content, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    public List<Chunk> chunk(String content, int chunkSize, int overlap) {
        List<Chunk> chunks = new ArrayList<>();
        String[] sentences = SENTENCE_BREAK.split(content);

        StringBuilder currentChunk = new StringBuilder();
        int chunkNumber = 0;
        int charCount = 0;
        int startIndex = 0;

        for (int i = 0; i < sentences.length; i++) {
            String sentence = sentences[i];

            if (charCount + sentence.length() > chunkSize && currentChunk.length() > 0) {
                // Save current chunk
                chunks.add(Chunk.builder()
                        .id("chunk_" + chunkNumber)
                        .text(currentChunk.toString().trim())
                        .startIndex(startIndex)
                        .endIndex(startIndex + currentChunk.length())
                        .section(detectSection(currentChunk.toString()))
                        .chunkNumber(chunkNumber)
                        .build());
                chunkNumber++;

                // Start new chunk with overlap
                String overlapText = getOverlapText(currentChunk.toString(), overlap);
                currentChunk = new StringBuilder(overlapText);
                startIndex = startIndex + currentChunk.length() - overlapText.length();
                charCount = overlapText.length();
            }

            currentChunk.append(sentence).append(" ");
            charCount += sentence.length() + 1;
        }

        // Save last chunk
        if (currentChunk.length() > 0) {
            chunks.add(Chunk.builder()
                    .id("chunk_" + chunkNumber)
                    .text(currentChunk.toString().trim())
                    .startIndex(startIndex)
                    .endIndex(startIndex + currentChunk.length())
                    .section(detectSection(currentChunk.toString()))
                    .chunkNumber(chunkNumber)
                    .build());
        }

        // Update totalChunks for all
        int total = chunks.size();
        chunks.forEach(c -> c.setTotalChunks(total));

        return chunks;
    }

    private String detectSection(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("introduction") || lower.contains("overview"))
            return "introduction";
        if (lower.contains("definition") || lower.contains("what is"))
            return "definition";
        if (lower.contains("example") || lower.contains("case study"))
            return "example";
        if (lower.contains("conclusion") || lower.contains("summary"))
            return "conclusion";
        if (lower.contains("strategy") || lower.contains("how to"))
            return "strategy";
        return "general";
    }

    private String getOverlapText(String text, int overlapChars) {
        if (text.length() <= overlapChars)
            return "";
        String overlap = text.substring(text.length() - overlapChars);
        // Start from first complete sentence in overlap
        int firstSentence = overlap.indexOf(". ");
        if (firstSentence > 0) {
            return overlap.substring(firstSentence + 2);
        }
        return overlap;
    }
}