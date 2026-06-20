package com.ventureverse.ventureverse_api.rag.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QdrantCollectionInitializer {

    @PostConstruct
    public void init() {

        System.out.println(
                "Qdrant collection initialization started"
        );

        // Collection creation will be added later

        System.out.println(
                "Qdrant collection initialization completed"
        );
    }
}