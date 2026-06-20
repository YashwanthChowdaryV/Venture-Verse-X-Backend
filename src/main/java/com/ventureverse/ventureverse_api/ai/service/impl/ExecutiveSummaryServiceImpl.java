package com.ventureverse.ventureverse_api.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ventureverse.ventureverse_api.ai.agent.ChiefAdvisorAgent;
import com.ventureverse.ventureverse_api.ai.client.OpenRouterClient;
import com.ventureverse.ventureverse_api.ai.dto.StartupExecutiveSummaryResponse;
import com.ventureverse.ventureverse_api.ai.service.ExecutiveSummaryService;
import com.ventureverse.ventureverse_api.util.JsonCleaner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ExecutiveSummaryServiceImpl
        implements ExecutiveSummaryService {

    private final ChiefAdvisorAgent chiefAdvisorAgent;

    private final OpenRouterClient openRouterClient;

    private final ObjectMapper objectMapper;

    @Override
    public StartupExecutiveSummaryResponse generateSummary(
            String investorAnalysis,
            String competitorAnalysis,
            String financeAnalysis,
            String customerAnalysis,
            String riskAnalysis,
            String productStrategyAnalysis) {

        try {

            String prompt =
                    chiefAdvisorAgent.buildPrompt(
                            investorAnalysis,
                            competitorAnalysis,
                            financeAnalysis,
                            customerAnalysis,
                            riskAnalysis,
                            productStrategyAnalysis
                    );

            String response =
                    openRouterClient.validateIdea(
                            prompt
                    );

            String cleanedResponse =
                    response
                            .replace("```json", "")
                            .replace("```", "")
                            .trim();

            System.out.println(
                    "\n===== EXECUTIVE SUMMARY RESPONSE ====="
            );
            System.out.println(cleanedResponse);
            System.out.println(
                    "=====================================\n"
            );

            cleanedResponse =
                    JsonCleaner.clean(
                            cleanedResponse
                    );

            return objectMapper.readValue(
                    cleanedResponse,
                    StartupExecutiveSummaryResponse.class
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Executive summary generation failed: "
                            + e.getMessage(),
                    e
            );
        }
    }
}