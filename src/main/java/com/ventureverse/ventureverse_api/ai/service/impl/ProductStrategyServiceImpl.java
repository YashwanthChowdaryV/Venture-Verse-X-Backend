package com.ventureverse.ventureverse_api.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureverse.ventureverse_api.ai.agent.ProductStrategyAgent;
import com.ventureverse.ventureverse_api.ai.client.OpenRouterClient;
import com.ventureverse.ventureverse_api.ai.dto.ProductStrategyRequest;
import com.ventureverse.ventureverse_api.ai.dto.ProductStrategyResponse;
import com.ventureverse.ventureverse_api.ai.service.ProductStrategyService;
import com.ventureverse.ventureverse_api.entities.ProductStrategyAnalysis;
import com.ventureverse.ventureverse_api.entities.Startup;
import com.ventureverse.ventureverse_api.entities.User;
import com.ventureverse.ventureverse_api.repositories.ProductStrategyAnalysisRepository;
import com.ventureverse.ventureverse_api.repositories.StartupRepository;
import com.ventureverse.ventureverse_api.repositories.UserRepository;
import com.ventureverse.ventureverse_api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ventureverse.ventureverse_api.rag.service.ContextBuilderService;
import com.ventureverse.ventureverse_api.util.JsonCleaner;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProductStrategyServiceImpl
        implements ProductStrategyService {

    private final ProductStrategyAgent productStrategyAgent;
    private final OpenRouterClient openRouterClient;
    private final StartupRepository startupRepository;
    private final UserRepository userRepository;
    private final ProductStrategyAnalysisRepository productStrategyAnalysisRepository;
    private final ObjectMapper objectMapper;
private final ContextBuilderService
        contextBuilderService;
    @Override
    public ProductStrategyResponse analyze(
            ProductStrategyRequest request) {

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
        productStrategyAgent.buildPrompt(
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

            ProductStrategyResponse response =
                    objectMapper.readValue(
                            cleanedResponse,
                            ProductStrategyResponse.class
                    );
            System.out.println("===== PRODUCT STRATEGY RESPONSE =====");
System.out.println(cleanedResponse);
System.out.println("Score = " + response.getScore());
System.out.println("Analysis = " + response.getAnalysis());
System.out.println("=====================================");

            ProductStrategyAnalysis analysis =
                    ProductStrategyAnalysis.builder()
                            .user(user)
                            .startup(startup)
                            .prompt(prompt)
                            .response(cleanedResponse)
                            .score(response.getScore())
                            .createdAt(LocalDateTime.now())
                            .build();

            productStrategyAnalysisRepository.save(
                    analysis
            );

            return response;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Product strategy analysis failed: "
                            + e.getMessage(),
                    e
            );
        }
    }
}