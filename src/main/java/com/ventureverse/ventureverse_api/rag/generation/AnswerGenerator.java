package com.ventureverse.ventureverse_api.rag.generation;

import com.ventureverse.ventureverse_api.rag.core.context.ContextCompressor;
import com.ventureverse.ventureverse_api.rag.core.context.ContextCompressor.CompressedContext;
import com.ventureverse.ventureverse_api.rag.generation.CitationGenerator.Citation;
import com.ventureverse.ventureverse_api.rag.generation.CitationGenerator.CitedAnswer;
import com.ventureverse.ventureverse_api.rag.retrieval.hybrid.HybridRetriever.ScoredDocument;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnswerGenerator {

    private final RestTemplate restTemplate;
    private final CitationGenerator citationGenerator;
    private final ContextCompressor contextCompressor;

    @Value("${openrouter.api.key}")
    private String apiKey;

    @Value("${openrouter.url}")
    private String apiUrl;

    private static final String MODEL = "deepseek/deepseek-chat-v3-0324";

    @Data
    @Builder
    public static class GeneratedAnswer {
        private String answer;
        private List<Citation> citations;
        private String model;
        private int tokensUsed;
        private double confidence;
    }

    public GeneratedAnswer generate(String query, List<ScoredDocument> documents) {
        // 1. Compress context from documents
        CompressedContext compressed = contextCompressor.compress(query, documents);
        String context = compressed.getCompressedText();

        // 2. Build system prompt
        String systemPrompt = buildSystemPrompt();

        // 3. Build user prompt
        String userPrompt = buildUserPrompt(query, context);

        // 4. Call DeepSeek
        String response = callLLM(systemPrompt, userPrompt);

        // 5. Generate citations
        CitedAnswer cited = citationGenerator.generateCitations(response, documents);

        return GeneratedAnswer.builder()
                .answer(response)
                .citations(cited.getCitations())
                .model(MODEL)
                .tokensUsed(estimateTokens(query + context + response))
                .confidence(cited.getOverallConfidence())
                .build();
    }

    private String buildSystemPrompt() {
        return "You are VentureVerseX AI, an enterprise startup intelligence assistant.\n\n" +
                "Guidelines:\n" +
                "- Answer accurately using ONLY the provided context\n" +
                "- Write in clean, professional paragraphs\n" +
                "- Do NOT use markdown formatting, asterisks, or bullet symbols\n" +
                "- Do NOT output headers or formatting symbols\n" +
                "- Use plain text with clear paragraph breaks\n" +
                "- If context lacks information, say 'I don't have enough information'\n" +
                "- Use clear, professional language suitable for founders and investors\n" +
                "- Never make up information not in the context\n\n" +
                "Your users are startup founders, investors, and analysts seeking " +
                "actionable business intelligence presented in clean, readable text.";
    }

    private String buildUserPrompt(String query, String context) {
        return String.format(
                "Context documents:\n%s\n\nQuestion: %s\n\n" +
                        "Provide a comprehensive answer based on the context above. " +
                        "Write in plain paragraphs without any markdown formatting.",
                context, query);
    }

    private String callLLM(String systemPrompt, String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> message1 = new LinkedHashMap<>();
        message1.put("role", "system");
        message1.put("content", systemPrompt);

        Map<String, Object> message2 = new LinkedHashMap<>();
        message2.put("role", "user");
        message2.put("content", userPrompt);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", MODEL);
        body.put("messages", List.of(message1, message2));
        body.put("temperature", 0.3);
        body.put("max_tokens", 1024);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                apiUrl, HttpMethod.POST, request, Map.class);

        Map<String, Object> choice = (Map<String, Object>) ((List<?>) response.getBody().get("choices")).get(0);
        Map<String, Object> message = (Map<String, Object>) choice.get("message");

        String content = message.get("content").toString();

        // Clean markdown and formatting artifacts
        content = content.replaceAll("\\*\\*", "")
                .replaceAll("(?m)^#{1,6}\\s", "")
                .replaceAll("(?m)^[-*]\\s", "• ")
                .replaceAll("\\s*\\[\\d+\\]", "")
                .replaceAll("\\s*\\[\\d+,\\s*\\d+\\]", "")
                .replaceAll("\\[Source:[^\\]]+\\]", "")
                .replaceAll("\\(Source:[^)]+\\)", "")
                .replaceAll("(?m)^\\s*\\n", "\n")
                .trim();

        return content;
    }

    private int estimateTokens(String text) {
        return (int) Math.ceil(text.length() / 4.0);
    }
}