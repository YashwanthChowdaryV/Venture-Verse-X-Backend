package com.ventureverse.ventureverse_api.rag.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class QdrantConfig {

    @Value("${qdrant.url}")
    private String qdrantUrl;

    @Value("${qdrant.api.key}")
    private String qdrantApiKey;
}