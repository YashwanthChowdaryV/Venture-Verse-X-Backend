package com.ventureverse.ventureverse_api.rag.service;

import java.util.List;
import java.util.Map;

public interface RagSearchService {

    /**
     * @deprecated Use {@link #searchWithContext(String)} for rich context with
     *             metadata.
     *             This method now retrieves 10 chunks instead of 5.
     */
    @Deprecated
    List<String> search(String query);

    /**
     * Searches the knowledge base and returns formatted context with metadata,
     * ready for injection into LLM prompts. Results are filtered by relevance score
     * and capped at MAX_CONTEXT_CHUNKS.
     *
     * @param query The user's query string
     * @return Formatted context string with document titles, IDs, categories, and
     *         relevance scores
     */
    String searchWithContext(String query);

    /**
     * Returns detailed search results including scores, titles, and document IDs
     * for debugging and monitoring retrieval quality. Retrieves 15 results for
     * better visibility into what the vector search is returning.
     *
     * @param query The user's query string
     * @return List of maps containing score, title, document_id, and content
     *         preview
     */
    List<Map<String, Object>> searchDebug(String query);

    /**
     * Searches with keyword boosting for queries that contain specific terms
     * that should be weighted more heavily.
     *
     * @param query    The user's query string
     * @param keywords List of keywords to boost
     * @return Formatted context string
     */
    String searchWithKeywords(String query, List<String> keywords);

    /**
     * Searches with metadata filtering (e.g., by category, stage, or personas).
     *
     * @param query    The user's query string
     * @param metadata Map of metadata fields to filter on
     * @return Formatted context string
     */
    String searchWithMetadataFilter(String query, Map<String, String> metadata);

    /**
     * Returns statistics about the last search for monitoring purposes.
     *
     * @param query The user's query string
     * @return Map containing query, total_results, max_score, avg_score, etc.
     */
    Map<String, Object> getSearchStats(String query);
}