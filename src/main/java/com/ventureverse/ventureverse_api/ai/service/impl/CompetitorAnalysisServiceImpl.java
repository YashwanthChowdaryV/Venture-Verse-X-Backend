package com.ventureverse.ventureverse_api.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureverse.ventureverse_api.ai.agent.CompetitorAgent;
import com.ventureverse.ventureverse_api.ai.client.OpenRouterClient;
import com.ventureverse.ventureverse_api.ai.dto.CompetitorValidationRequest;
import com.ventureverse.ventureverse_api.ai.dto.CompetitorValidationResponse;
import com.ventureverse.ventureverse_api.ai.service.CompetitorAnalysisService;
import com.ventureverse.ventureverse_api.entities.CompetitorAnalysis;
import com.ventureverse.ventureverse_api.entities.Startup;
import com.ventureverse.ventureverse_api.entities.User;
import com.ventureverse.ventureverse_api.repositories.CompetitorAnalysisRepository;
import com.ventureverse.ventureverse_api.repositories.StartupRepository;
import com.ventureverse.ventureverse_api.repositories.UserRepository;
import com.ventureverse.ventureverse_api.security.SecurityUtils;
import com.ventureverse.ventureverse_api.util.JsonCleaner;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ventureverse.ventureverse_api.rag.service.ContextBuilderService;


import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CompetitorAnalysisServiceImpl
        implements CompetitorAnalysisService {

    private final CompetitorAgent competitorAgent;
    private final OpenRouterClient openRouterClient;
    private final StartupRepository startupRepository;
    private final UserRepository userRepository;
    private final CompetitorAnalysisRepository competitorAnalysisRepository;
    private final ObjectMapper objectMapper;
    private final ContextBuilderService
        contextBuilderService;

    @Override
    public CompetitorValidationResponse analyze(
            CompetitorValidationRequest request) {

        try {

            String email =
                    SecurityUtils.getCurrentUserEmail();

            User user =
                    userRepository.findByEmail(email)
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "User not found"
                                    ));

            Startup startup =
                    startupRepository.findById(
                                    request.getStartupId()
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Startup not found"
                                    ));

String ragContext =
        contextBuilderService
                .buildContext(
                        startup.getIdeaDescription()
                );

String prompt =
        competitorAgent.buildPrompt(
                startup.getStartupName(),
                startup.getIdeaDescription(),
                startup.getIndustry(),
                startup.getTargetMarket()
        );

prompt =
        ragContext
                + "\n\n"
                + prompt;

            String aiResponse =
                    openRouterClient.validateIdea(prompt);

String cleanedResponse = aiResponse
        .replace("```json", "")
        .replace("```", "")
        .trim();

cleanedResponse =
        JsonCleaner.clean(
                cleanedResponse
        );
CompetitorValidationResponse response =


        objectMapper.readValue(
                cleanedResponse,
                CompetitorValidationResponse.class
        );
            CompetitorAnalysis analysis =
                    CompetitorAnalysis.builder()
                            .user(user)
                            .startup(startup)
                            .prompt(prompt)
                            .response(aiResponse)
                            .score(response.getScore())
                            .createdAt(LocalDateTime.now())
                            .build();

            competitorAnalysisRepository.save(
                    analysis
            );

            return response;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Competitor analysis failed: "
                            + e.getMessage(),
                    e
            );
        }
    }
}