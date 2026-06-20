package com.ventureverse.ventureverse_api.ai.orchestrator.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureverse.ventureverse_api.ai.agent.CustomerAgent;
import com.ventureverse.ventureverse_api.ai.client.OpenRouterClient;
import com.ventureverse.ventureverse_api.ai.dto.CustomerValidationRequest;
import com.ventureverse.ventureverse_api.ai.dto.CustomerValidationResponse;
import com.ventureverse.ventureverse_api.ai.service.CustomerAnalysisService;
import com.ventureverse.ventureverse_api.entities.CustomerAnalysis;
import com.ventureverse.ventureverse_api.entities.Startup;
import com.ventureverse.ventureverse_api.entities.User;
import com.ventureverse.ventureverse_api.repositories.CustomerAnalysisRepository;
import com.ventureverse.ventureverse_api.repositories.StartupRepository;
import com.ventureverse.ventureverse_api.repositories.UserRepository;
import com.ventureverse.ventureverse_api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomerAnalysisServiceImpl
        implements CustomerAnalysisService {

    private final CustomerAgent customerAgent;
    private final OpenRouterClient openRouterClient;
    private final StartupRepository startupRepository;
    private final UserRepository userRepository;
    private final CustomerAnalysisRepository customerAnalysisRepository;
    private final ObjectMapper objectMapper;

    @Override
    public CustomerValidationResponse analyze(
            CustomerValidationRequest request) {

        try {

            String email =
                    SecurityUtils.getCurrentUserEmail();

            if (email == null || email.isBlank()) {
                throw new RuntimeException(
                        "No authenticated user found"
                );
            }

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

            String prompt =
                    customerAgent.buildPrompt(
                            startup.getStartupName(),
                            startup.getIdeaDescription(),
                            startup.getIndustry(),
                            startup.getTargetMarket()
                    );

            String aiResponse =
                    openRouterClient.validateIdea(prompt);

            String cleanedResponse =
                    aiResponse
                            .replace("```json", "")
                            .replace("```", "")
                            .trim();

            CustomerValidationResponse response =
                    objectMapper.readValue(
                            cleanedResponse,
                            CustomerValidationResponse.class
                    );

            CustomerAnalysis analysis =
                    CustomerAnalysis.builder()
                            .user(user)
                            .startup(startup)
                            .prompt(prompt)
                            .response(cleanedResponse)
                            .score(response.getScore())
                            .createdAt(LocalDateTime.now())
                            .build();

            customerAnalysisRepository.save(
                    analysis
            );

            return response;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Customer analysis failed: "
                            + e.getMessage(),
                    e
            );
        }
    }
}