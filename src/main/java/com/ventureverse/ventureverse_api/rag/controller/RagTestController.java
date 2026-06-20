package com.ventureverse.ventureverse_api.rag.controller;

import com.ventureverse.ventureverse_api.rag.service.ContextBuilderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rag")
@RequiredArgsConstructor
public class RagTestController {

    private final ContextBuilderService
            contextBuilderService;

    @GetMapping("/context")
    public String context(
            @RequestParam String query) {

        return contextBuilderService
                .buildContext(query);
    }
}