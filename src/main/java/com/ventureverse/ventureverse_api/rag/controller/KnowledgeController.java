package com.ventureverse.ventureverse_api.rag.controller;

import com.ventureverse.ventureverse_api.rag.dto.KnowledgeUploadRequest;
import com.ventureverse.ventureverse_api.rag.service.KnowledgeBaseService;
import com.ventureverse.ventureverse_api.rag.service.RagSearchService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeBaseService
            knowledgeBaseService;
    private final RagSearchService
        ragSearchService;


@GetMapping("/search")
public Object search(
        @RequestParam String query) {

    return ragSearchService
            .search(query);
}


    @PostMapping("/upload")
    public String uploadDocument(
            @RequestBody
            KnowledgeUploadRequest request) {

        knowledgeBaseService
                .uploadDocument(
                        request
                );


        return "Document processed";
    }
}