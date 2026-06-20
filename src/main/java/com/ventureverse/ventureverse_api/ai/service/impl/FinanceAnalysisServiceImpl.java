package com.ventureverse.ventureverse_api.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureverse.ventureverse_api.ai.agent.FinanceAgent;
import com.ventureverse.ventureverse_api.ai.client.OpenRouterClient;
import com.ventureverse.ventureverse_api.ai.dto.FinanceValidationRequest;
import com.ventureverse.ventureverse_api.ai.dto.FinanceValidationResponse;
import com.ventureverse.ventureverse_api.ai.service.FinanceAnalysisService;
import com.ventureverse.ventureverse_api.entities.FinanceAnalysis;
import com.ventureverse.ventureverse_api.entities.Startup;
import com.ventureverse.ventureverse_api.entities.User;
import com.ventureverse.ventureverse_api.repositories.FinanceAnalysisRepository;
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
public class FinanceAnalysisServiceImpl
        implements FinanceAnalysisService {

    private final FinanceAgent financeAgent;
    private final OpenRouterClient openRouterClient;
    private final StartupRepository startupRepository;
    private final UserRepository userRepository;
    private final FinanceAnalysisRepository financeAnalysisRepository;
    private final ObjectMapper objectMapper;
            private final ContextBuilderService
        contextBuilderService;
    @Override
    public FinanceValidationResponse analyze(
            FinanceValidationRequest request) {

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
                                    request.getStartupId())
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
        financeAgent.buildPrompt(
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

            String cleanedResponse =
                    aiResponse
                            .replace("```json", "")
                            .replace("```", "")
                            .trim();
            cleanedResponse =
        JsonCleaner.clean(
                cleanedResponse
        );

            FinanceValidationResponse response =
                    objectMapper.readValue(
                            cleanedResponse,
                            FinanceValidationResponse.class
                    );

            FinanceAnalysis analysis =
                    FinanceAnalysis.builder()
                            .user(user)
                            .startup(startup)
                            .prompt(prompt)
                            .response(cleanedResponse)
                            .score(response.getScore())
                            .createdAt(LocalDateTime.now())
                            .build();

            financeAnalysisRepository.save(
                    analysis
            );

            return response;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Finance analysis failed: "
                            + e.getMessage(),
                    e
            );
        }
    }
}