package com.ventureverse.ventureverse_api.rag.service;

import java.util.List;

public interface VectorStoreService {

    void storeDocument(
            String id,
            String title,
            String content,
            List<Float> embedding
    );

    List<String> search(
            String query
    );
}