package com.ventureverse.ventureverse_api.rag.service;

import com.ventureverse.ventureverse_api.rag.dto.KnowledgeUploadRequest;

public interface KnowledgeBaseService {

    void uploadDocument(
            KnowledgeUploadRequest request
    );
}