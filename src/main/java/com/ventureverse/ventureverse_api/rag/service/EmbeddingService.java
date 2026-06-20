package com.ventureverse.ventureverse_api.rag.service;

import java.util.List;

public interface EmbeddingService {

    List<Float> createEmbedding(
            String text
    );
}