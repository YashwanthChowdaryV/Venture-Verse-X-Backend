package com.ventureverse.ventureverse_api.ai.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OpenRouterClientImpl implements OpenRouterClient {

        private final RestTemplate restTemplate;

        @Value("${openrouter.api.key}")
        private String apiKey;

        @Value("${openrouter.url}")
        private String url;

        @Override
        public String validateIdea(String prompt) {

                HttpHeaders headers = new HttpHeaders();

                headers.setBearerAuth(apiKey);
                headers.setContentType(MediaType.APPLICATION_JSON);

                Map<String, Object> requestBody = Map.of(
                                "model", "deepseek/deepseek-chat-v3-0324",
                                "max_tokens", 2500,
                                "temperature", 0.3,
                                "messages", List.of(
                                                Map.of(
                                                                "role", "user",
                                                                "content", prompt)));

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map> response = restTemplate.exchange(
                                url,
                                HttpMethod.POST,
                                entity,
                                Map.class);

                System.out.println(
                                "\n===== OPENROUTER RESPONSE =====");
                System.out.println(
                                "HTTP Status = "
                                                + response.getStatusCode());
                System.out.println(
                                "Body = "
                                                + response.getBody());
                System.out.println(
                                "===============================\n");

                Map<String, Object> responseBody = response.getBody();

                if (responseBody == null) {
                        throw new RuntimeException(
                                        "OpenRouter returned empty response");
                }

                Object choicesObj = responseBody.get("choices");

                if (choicesObj == null) {
                        throw new RuntimeException(
                                        "OpenRouter Error Response: "
                                                        + responseBody);
                }

                List<?> choices = (List<?>) choicesObj;

                if (choices.isEmpty()) {
                        throw new RuntimeException(
                                        "OpenRouter returned no choices");
                }

                Map<?, ?> choice = (Map<?, ?>) choices.get(0);

                Object messageObj = choice.get("message");

                if (messageObj == null) {
                        throw new RuntimeException(
                                        "OpenRouter returned no message: "
                                                        + responseBody);
                }

                Map<?, ?> message = (Map<?, ?>) messageObj;

                Object content = message.get("content");

                if (content == null) {
                        throw new RuntimeException(
                                        "OpenRouter returned no content: "
                                                        + responseBody);
                }

                return content.toString();
        }
}