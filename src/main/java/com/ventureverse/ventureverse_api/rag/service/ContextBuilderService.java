package com.ventureverse.ventureverse_api.rag.service;

public interface ContextBuilderService {

    String buildContext(
            String query
    );
}