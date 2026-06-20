package com.ventureverse.ventureverse_api.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureverse.ventureverse_api.ai.agent.InvestorAgent;
import com.ventureverse.ventureverse_api.ai.client.OpenRouterClient;
import com.ventureverse.ventureverse_api.ai.dto.AnalysisHistoryResponse;
import com.ventureverse.ventureverse_api.ai.dto.IdeaValidationRequest;
import com.ventureverse.ventureverse_api.ai.dto.IdeaValidationResponse;
import com.ventureverse.ventureverse_api.ai.service.IdeaValidationService;
import com.ventureverse.ventureverse_api.entities.Analysis;
import com.ventureverse.ventureverse_api.entities.Startup;
import com.ventureverse.ventureverse_api.entities.User;
import com.ventureverse.ventureverse_api.rag.service.ContextBuilderService;
import com.ventureverse.ventureverse_api.repositories.AnalysisRepository;
import com.ventureverse.ventureverse_api.repositories.StartupRepository;
import com.ventureverse.ventureverse_api.repositories.UserRepository;
import com.ventureverse.ventureverse_api.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IdeaValidationServiceImpl
                implements IdeaValidationService {

        private final InvestorAgent investorAgent;
        private final OpenRouterClient openRouterClient;
        private final StartupRepository startupRepository;
        private final UserRepository userRepository;
        private final AnalysisRepository analysisRepository;
        private final ObjectMapper objectMapper;
        private final ContextBuilderService contextBuilderService;

        @Override
        public IdeaValidationResponse validateIdea(
                        IdeaValidationRequest request) {

                try {

                        String email = SecurityUtils.getCurrentUserEmail();

                        if (email == null || email.isBlank()) {
                                throw new RuntimeException(
                                                "No authenticated user found");
                        }

                        User user = userRepository.findByEmail(email)
                                        .orElseThrow(() -> new RuntimeException(
                                                        "User not found: "
                                                                        + email));

                        Startup startup = startupRepository.findById(
                                        request.getStartupId())
                                        .orElseThrow(() -> new RuntimeException(
                                                        "Startup not found with ID: "
                                                                        + request.getStartupId()));

                        String ragContext = contextBuilderService
                                        .buildContext(
                                                        startup.getIdeaDescription());

                        String prompt = investorAgent.buildPrompt(
                                        startup.getStartupName(),
                                        startup.getIdeaDescription(),
                                        startup.getIndustry(),
                                        startup.getTargetMarket());

                        prompt = ragContext
                                        + "\n\n"
                                        + prompt;

                        String aiResponse = openRouterClient.validateIdea(prompt);
                        System.out.println("\n========== INVESTOR AI RESPONSE ==========");
                        System.out.println(aiResponse);
                        System.out.println("==========================================\n");

                        String cleanedResponse = aiResponse
                                        .replace("```json", "")
                                        .replace("```", "")
                                        .trim();

                        IdeaValidationResponse response = objectMapper.readValue(
                                        cleanedResponse,
                                        IdeaValidationResponse.class);

                        Analysis analysis = Analysis.builder()
                                        .user(user)
                                        .startup(startup)
                                        .prompt(prompt)
                                        .response(aiResponse)
                                        .score(response.getScore())
                                        .createdAt(LocalDateTime.now())
                                        .build();

                        analysisRepository.save(analysis);

                        return response;

                } catch (Exception e) {

                        throw new RuntimeException(
                                        "AI validation failed: "
                                                        + e.getMessage(),
                                        e);
                }
        }

        @Override
        public List<AnalysisHistoryResponse> getHistory(
                        Long startupId) {

                Startup startup = startupRepository.findById(startupId)
                                .orElseThrow(() -> new RuntimeException(
                                                "Startup not found"));

                return analysisRepository
                                .findByStartupIdOrderByCreatedAtDesc(
                                                startup.getId())
                                .stream()
                                .map(analysis -> AnalysisHistoryResponse
                                                .builder()
                                                .id(analysis.getId())
                                                .score(analysis.getScore())
                                                .response(
                                                                analysis.getResponse())
                                                .createdAt(
                                                                analysis.getCreatedAt())
                                                .build())
                                .toList();
        }
}