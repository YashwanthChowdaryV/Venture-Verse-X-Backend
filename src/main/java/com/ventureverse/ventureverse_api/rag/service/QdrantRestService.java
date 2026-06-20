package com.ventureverse.ventureverse_api.rag.service;

import java.util.List;
import java.util.Map;

public interface QdrantRestService {

        void createCollection();

        List<String> search(List<Float> vector);

        List<Map<String, Object>> searchWithScores(List<Float> vector, int limit);

        // ADD THESE TWO METHOD SIGNATURES:
        List<Map<String, Object>> searchWithRichContext(List<Float> vector, int limit);

        List<Map<String, Object>> searchWithMetadataFilter(List<Float> vector, int limit, Map<String, String> metadata);

        List<Map<String, Object>> searchWithKeywordBoost(List<Float> vector, List<String> keywords, int limit);

        void insertVector(String id, String title, String content, List<Float> vector);

        void insertVectorWithMetadata(String id, String title, String content, List<Float> vector,
                        Map<String, Object> metadata);
}