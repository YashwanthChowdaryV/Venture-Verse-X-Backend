package com.ventureverse.ventureverse_api.rag.service.impl;

import com.ventureverse.ventureverse_api.rag.service.ContextBuilderService;
import com.ventureverse.ventureverse_api.rag.service.RagSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContextBuilderServiceImpl
        implements ContextBuilderService {

    private final RagSearchService
            ragSearchService;

    @Override
    public String buildContext(
            String query) {

        List<String> docs =
                ragSearchService.search(query);

        StringBuilder context =
                new StringBuilder();

        context.append(
                "KNOWLEDGE BASE CONTEXT:\n\n"
        );

        int count = 0;

        for (String doc : docs) {

            if (count >= 3) {
                break;
            }

            context.append(doc)
                    .append("\n\n");

            count++;
        }

        return context.toString();
    }
}