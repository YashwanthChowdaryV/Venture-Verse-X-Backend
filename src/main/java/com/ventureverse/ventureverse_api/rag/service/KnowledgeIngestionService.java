package com.ventureverse.ventureverse_api.rag.service;

import com.ventureverse.ventureverse_api.rag.dto.IngestionResponse;
import com.ventureverse.ventureverse_api.rag.dto.KnowledgeUploadRequest;

import java.util.List;

public interface KnowledgeIngestionService {

    /**
     * Ingests all knowledge base files from the configured directory
     * 
     * @return IngestionResponse with details of what was processed
     */
    IngestionResponse ingestKnowledgeBase();

    /**
     * Uploads a single document with metadata
     * 
     * @param request The knowledge upload request containing content and metadata
     * @return IngestionResponse with details of the upload
     */
    IngestionResponse uploadDocument(KnowledgeUploadRequest request);

    /**
     * Uploads multiple documents in batch
     * 
     * @param requests List of knowledge upload requests
     * @return IngestionResponse with aggregated details
     */
    IngestionResponse uploadDocuments(List<KnowledgeUploadRequest> requests);

    // Optional: Additional methods you might find useful

    /**
     * Ingests a specific file by path
     * 
     * @param filePath Path to the file to ingest
     * @return IngestionResponse with details of the upload
     */
    // IngestionResponse ingestFile(String filePath);

    /**
     * Deletes all documents in the knowledge base
     * 
     * @return boolean indicating success
     */
    // boolean clearKnowledgeBase();

    /**
     * Gets the count of documents currently stored
     * 
     * @return number of documents
     */
    // long getDocumentCount();
}