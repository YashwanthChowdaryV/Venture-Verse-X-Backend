package com.ventureverse.ventureverse_api.rag.service.impl;

import com.ventureverse.ventureverse_api.rag.service.VectorStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QdrantVectorStoreServiceImpl
        implements VectorStoreService {

    @Override
    public void storeDocument(
            String id,
            String title,
            String content,
            List<Float> embedding) {

        System.out.println(
                "Document ready for Qdrant insertion"
        );

        System.out.println(
                "ID = " + id
        );

        System.out.println(
                "Title = " + title
        );

        System.out.println(
                "Vector Size = "
                        + embedding.size()
        );
    }

    @Override
    public List<String> search(
            String query) {

        System.out.println(
                "Searching Qdrant for: "
                        + query
        );

        return Collections.emptyList();
    }
}