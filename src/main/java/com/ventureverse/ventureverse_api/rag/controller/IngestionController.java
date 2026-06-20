package com.ventureverse.ventureverse_api.rag.controller;

import com.ventureverse.ventureverse_api.rag.dto.IngestionResponse;
import com.ventureverse.ventureverse_api.rag.service.KnowledgeIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class IngestionController {

    private final KnowledgeIngestionService
            ingestionService;

    @PostMapping("/ingest")
    public IngestionResponse ingest() {

        return ingestionService
                .ingestKnowledgeBase();
    }
}