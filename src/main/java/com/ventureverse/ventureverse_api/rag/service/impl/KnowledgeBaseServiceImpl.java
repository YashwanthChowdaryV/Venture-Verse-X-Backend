package com.ventureverse.ventureverse_api.rag.service.impl;

import com.ventureverse.ventureverse_api.rag.dto.KnowledgeUploadRequest;
import com.ventureverse.ventureverse_api.rag.service.EmbeddingService;
import com.ventureverse.ventureverse_api.rag.service.KnowledgeBaseService;
import com.ventureverse.ventureverse_api.rag.service.QdrantRestService;
import com.ventureverse.ventureverse_api.rag.service.VectorStoreService;
import com.ventureverse.ventureverse_api.rag.service.QdrantRestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl
        implements KnowledgeBaseService {

    private final EmbeddingService embeddingService;

    private final VectorStoreService
            vectorStoreService;
    private final QdrantRestService
        qdrantRestService;

    @Override
    public void uploadDocument(
            KnowledgeUploadRequest request) {

        var embedding =
                embeddingService
                        .createEmbedding(
                                request.getContent()
                        );

        String id =
                UUID.randomUUID()
                        .toString();
                    
        qdrantRestService.createCollection();

qdrantRestService.insertVector(
        id,
        request.getTitle(),
        request.getContent(),
        embedding
);

        System.out.println(
                "Embedding size = "
                        + embedding.size()
        );
    }
}