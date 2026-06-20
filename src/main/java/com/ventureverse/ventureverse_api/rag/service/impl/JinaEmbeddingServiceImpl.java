package com.ventureverse.ventureverse_api.rag.service.impl;

import com.ventureverse.ventureverse_api.rag.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class JinaEmbeddingServiceImpl
        implements EmbeddingService {

    private final RestTemplate restTemplate;

    @Value("${jina.api.key}")
    private String apiKey;

    @Value("${jina.embedding.url}")
    private String embeddingUrl;

    @Override
    public List<Float> createEmbedding(
            String text) {

        HttpHeaders headers =
                new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON);

        headers.setBearerAuth(apiKey);

        Map<String, Object> body =
                new HashMap<>();

        body.put(
                "model",
                "jina-embeddings-v3"
        );

        body.put(
                "input",
                List.of(text)
        );

        HttpEntity<Map<String, Object>>
                request =
                new HttpEntity<>(
                        body,
                        headers
                );

        ResponseEntity<Map> response =
                restTemplate.exchange(
                        embeddingUrl,
                        HttpMethod.POST,
                        request,
                        Map.class
                );

        List<?> data =
                (List<?>) response
                        .getBody()
                        .get("data");

        Map<?, ?> first =
                (Map<?, ?>) data.get(0);

        List<Double> vector =
                (List<Double>)
                        first.get(
                                "embedding");

        List<Float> result =
                new ArrayList<>();

        for (Double d : vector) {
            result.add(
                    d.floatValue()
            );
        }

        return result;
    }
}