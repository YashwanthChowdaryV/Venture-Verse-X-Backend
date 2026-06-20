package com.ventureverse.ventureverse_api.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureverse.ventureverse_api.ai.agent.RiskAgent;
import com.ventureverse.ventureverse_api.ai.client.OpenRouterClient;
import com.ventureverse.ventureverse_api.ai.dto.RiskValidationRequest;
import com.ventureverse.ventureverse_api.ai.dto.RiskValidationResponse;
import com.ventureverse.ventureverse_api.ai.service.RiskAnalysisService;
import com.ventureverse.ventureverse_api.entities.RiskAnalysis;
import com.ventureverse.ventureverse_api.entities.Startup;
import com.ventureverse.ventureverse_api.entities.User;
import com.ventureverse.ventureverse_api.repositories.RiskAnalysisRepository;
import com.ventureverse.ventureverse_api.repositories.StartupRepository;
import com.ventureverse.ventureverse_api.repositories.UserRepository;
import com.ventureverse.ventureverse_api.security.SecurityUtils;
import com.ventureverse.ventureverse_api.util.JsonCleaner;
import com.ventureverse.ventureverse_api.rag.service.ContextBuilderService;
import lombok.RequiredArgsConstructor;
import com.ventureverse.ventureverse_api.util.JsonCleaner;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RiskAnalysisServiceImpl
        implements RiskAnalysisService {

    private final RiskAgent riskAgent;
    private final OpenRouterClient openRouterClient;
    private final StartupRepository startupRepository;
    private final UserRepository userRepository;
    private final RiskAnalysisRepository riskAnalysisRepository;
    private final ObjectMapper objectMapper;
private final ContextBuilderService
        contextBuilderService;
    @Override
    public RiskValidationResponse analyze(
            RiskValidationRequest request) {

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
        riskAgent.buildPrompt(
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

            RiskValidationResponse response =
                    objectMapper.readValue(
                            cleanedResponse,
                            RiskValidationResponse.class
                    );

            RiskAnalysis analysis =
                    RiskAnalysis.builder()
                            .user(user)
                            .startup(startup)
                            .prompt(prompt)
                            .response(cleanedResponse)
                            .score(response.getScore())
                            .createdAt(LocalDateTime.now())
                            .build();

            riskAnalysisRepository.save(
                    analysis
            );

            return response;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Risk analysis failed: "
                            + e.getMessage(),
                    e
            );
        }
    }
}