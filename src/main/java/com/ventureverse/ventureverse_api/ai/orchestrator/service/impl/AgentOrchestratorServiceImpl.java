package com.ventureverse.ventureverse_api.ai.orchestrator.service.impl;

import com.ventureverse.ventureverse_api.ai.dto.*;
import com.ventureverse.ventureverse_api.ai.orchestrator.dto.StartupFullAnalysisResponse;
import com.ventureverse.ventureverse_api.ai.orchestrator.dto.StartupReportHistoryResponse;
import com.ventureverse.ventureverse_api.ai.orchestrator.service.AgentOrchestratorService;
import com.ventureverse.ventureverse_api.ai.service.CompetitorAnalysisService;
import com.ventureverse.ventureverse_api.ai.service.CustomerAnalysisService;
import com.ventureverse.ventureverse_api.ai.service.FinanceAnalysisService;
import com.ventureverse.ventureverse_api.ai.service.IdeaValidationService;
import com.ventureverse.ventureverse_api.ai.service.ProductStrategyService;
import com.ventureverse.ventureverse_api.ai.service.RiskAnalysisService;
import com.ventureverse.ventureverse_api.entities.Startup;
import com.ventureverse.ventureverse_api.ai.service.ExecutiveSummaryService;
import com.ventureverse.ventureverse_api.entities.StartupReport;
import com.ventureverse.ventureverse_api.entities.User;
import com.ventureverse.ventureverse_api.repositories.StartupReportRepository;
import com.ventureverse.ventureverse_api.repositories.StartupRepository;
import com.ventureverse.ventureverse_api.repositories.UserRepository;
import com.ventureverse.ventureverse_api.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentOrchestratorServiceImpl
        implements AgentOrchestratorService {

    private final IdeaValidationService investorService;
    private final CompetitorAnalysisService competitorService;
    private final FinanceAnalysisService financeService;
    private final CustomerAnalysisService customerService;
    private final RiskAnalysisService riskService;
    private final ProductStrategyService productStrategyService;
    private final StartupRepository startupRepository;
    private final UserRepository userRepository;
    private final StartupReportRepository startupReportRepository;
    private final ObjectMapper objectMapper;
    private final ExecutiveSummaryService
        executiveSummaryService;

    @Override
    public StartupFullAnalysisResponse analyzeStartup(
            Long startupId) {

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
                startupRepository.findById(startupId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Startup not found"
                                ));

        IdeaValidationRequest investorRequest =
                new IdeaValidationRequest();
        investorRequest.setStartupId(startupId);

        CompetitorValidationRequest competitorRequest =
                new CompetitorValidationRequest();
        competitorRequest.setStartupId(startupId);

        FinanceValidationRequest financeRequest =
                new FinanceValidationRequest();
        financeRequest.setStartupId(startupId);

        CustomerValidationRequest customerRequest =
                new CustomerValidationRequest();
        customerRequest.setStartupId(startupId);

        RiskValidationRequest riskRequest =
                new RiskValidationRequest();
        riskRequest.setStartupId(startupId);
        ProductStrategyRequest productRequest =
        new ProductStrategyRequest();

productRequest.setStartupId(startupId);

        IdeaValidationResponse investor =
                investorService.validateIdea(
                        investorRequest);

        CompetitorValidationResponse competitor =
                competitorService.analyze(
                        competitorRequest);

        FinanceValidationResponse finance =
                financeService.analyze(
                        financeRequest);

        CustomerValidationResponse customer =
                customerService.analyze(
                        customerRequest);

        RiskValidationResponse risk =
                riskService.analyze(
                        riskRequest);
        ProductStrategyResponse product =
        productStrategyService.analyze(
                productRequest
        );
        StartupExecutiveSummaryResponse executiveSummary =
        executiveSummaryService.generateSummary(
                investor.getAnalysis(),
                competitor.getAnalysis(),
                finance.getAnalysis(),
                customer.getAnalysis(),
                risk.getAnalysis(),
                product.getAnalysis()
        );

int overallScore =
(
        investor.getScore()
        + competitor.getScore()
        + finance.getScore()
        + customer.getScore()
        + risk.getScore()
        + product.getScore()
) / 6;

        String verdict;

        if (overallScore >= 80) {
            verdict = "Strong Startup Opportunity";
        } else if (overallScore >= 60) {
            verdict = "Promising But Needs Validation";
        } else {
            verdict = "High Risk Opportunity";
        }

StartupReport report =
        StartupReport.builder()

                .overallScore(overallScore)

                .investmentScore(
                        investor.getScore())

                .competitionScore(
                        competitor.getScore())

                .financialScore(
                        finance.getScore())

                .customerScore(
                        customer.getScore())

                .riskScore(
                        risk.getScore())

                .productStrategyScore(
                        product.getScore())

                .startupReadinessScore(
                        executiveSummary
                                .getStartupReadinessScore())

                .finalVerdict(
                        verdict)

                .finalRecommendation(
                        executiveSummary
                                .getFinalRecommendation())

                .executiveSummary(
                        executiveSummary
                                .getExecutiveSummary())

                .investorAnalysis(
                        investor.getAnalysis())

                .competitorAnalysis(
                        competitor.getAnalysis())

                .financeAnalysis(
                        finance.getAnalysis())

                .customerAnalysis(
                        customer.getAnalysis())

                .riskAnalysis(
                        risk.getAnalysis())

                .productStrategyAnalysis(
                        product.getAnalysis())

                .investorDetailsJson(
                        objectMapper.writeValueAsString(
                                investor))

                .competitorDetailsJson(
                        objectMapper.writeValueAsString(
                                competitor))

                .financeDetailsJson(
                        objectMapper.writeValueAsString(
                                finance))

                .customerDetailsJson(
                        objectMapper.writeValueAsString(
                                customer))

                .riskDetailsJson(
                        objectMapper.writeValueAsString(
                                risk))

                .productStrategyDetailsJson(
                        objectMapper.writeValueAsString(
                                product))

                .executiveSummaryJson(
                        objectMapper.writeValueAsString(
                                executiveSummary))

                .startup(startup)

                .user(user)

                .createdAt(
                        LocalDateTime.now())

                .build();
        System.out.println("PRODUCT SCORE BEFORE SAVE = "
        + product.getScore());

System.out.println("PRODUCT ANALYSIS BEFORE SAVE = "
        + product.getAnalysis());

System.out.println("REPORT SCORE BEFORE SAVE = "
        + report.getProductStrategyScore());

System.out.println("REPORT ANALYSIS BEFORE SAVE = "
        + report.getProductStrategyAnalysis());

        startupReportRepository.save(report);

return StartupFullAnalysisResponse
        .builder()
        .overallScore(overallScore)

        .investmentScore(
                investor.getScore())
        .competitionScore(
                competitor.getScore())
        .financialScore(
                finance.getScore())
        .customerScore(
                customer.getScore())
        .riskScore(
                risk.getScore())
        .productStrategyScore(
                product.getScore())

        .finalVerdict(verdict)

        .investorAnalysis(
                investor.getAnalysis())
        .competitorAnalysis(
                competitor.getAnalysis())
        .financeAnalysis(
                finance.getAnalysis())
        .customerAnalysis(
                customer.getAnalysis())
        .riskAnalysis(
                risk.getAnalysis())
        .productStrategyAnalysis(
                product.getAnalysis())

        .investorDetails(
                investor)
        .competitorDetails(
                competitor)
        .financeDetails(
                finance)
        .customerDetails(
                customer)
        .riskDetails(
                risk)
        .productStrategyDetails(
                product)
        .executiveSummary(
                executiveSummary
        )
        .build();
    }
         catch (Exception e) {

        throw new RuntimeException(
                "Startup analysis failed: "
                        + e.getMessage(),
                e
        );
    }}


    @Override
    public List<StartupReportHistoryResponse>
    getReportHistory(Long startupId) {

        return startupReportRepository
                .findByStartupIdOrderByCreatedAtDesc(
                        startupId
                )
                .stream()
                .map(report ->
                        StartupReportHistoryResponse
                                .builder()
                                .id(report.getId())
                                .overallScore(
                                        report.getOverallScore())
                                .finalVerdict(
                                        report.getFinalVerdict())
                                .createdAt(
                                        report.getCreatedAt())
                                .build()
                )
                .toList();
    }

    @Override
    public StartupFullAnalysisResponse
    getReport(Long reportId) {

        StartupReport report =
                startupReportRepository
                        .findById(reportId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Report not found"
                                ));

        IdeaValidationResponse investorDetails = null;
        CompetitorValidationResponse competitorDetails = null;
        FinanceValidationResponse financeDetails = null;
        CustomerValidationResponse customerDetails = null;
        RiskValidationResponse riskDetails = null;
        ProductStrategyResponse productStrategyDetails = null;
        StartupExecutiveSummaryResponse executiveSummary = null;

        try {
            if (report.getInvestorDetailsJson() != null) {
                investorDetails = objectMapper.readValue(report.getInvestorDetailsJson(), IdeaValidationResponse.class);
            }
        } catch (Exception e) {}
        try {
            if (report.getCompetitorDetailsJson() != null) {
                competitorDetails = objectMapper.readValue(report.getCompetitorDetailsJson(), CompetitorValidationResponse.class);
            }
        } catch (Exception e) {}
        try {
            if (report.getFinanceDetailsJson() != null) {
                financeDetails = objectMapper.readValue(report.getFinanceDetailsJson(), FinanceValidationResponse.class);
            }
        } catch (Exception e) {}
        try {
            if (report.getCustomerDetailsJson() != null) {
                customerDetails = objectMapper.readValue(report.getCustomerDetailsJson(), CustomerValidationResponse.class);
            }
        } catch (Exception e) {}
        try {
            if (report.getRiskDetailsJson() != null) {
                riskDetails = objectMapper.readValue(report.getRiskDetailsJson(), RiskValidationResponse.class);
            }
        } catch (Exception e) {}
        try {
            if (report.getProductStrategyDetailsJson() != null) {
                productStrategyDetails = objectMapper.readValue(report.getProductStrategyDetailsJson(), ProductStrategyResponse.class);
            }
        } catch (Exception e) {}
        try {
            if (report.getExecutiveSummaryJson() != null) {
                executiveSummary = objectMapper.readValue(report.getExecutiveSummaryJson(), StartupExecutiveSummaryResponse.class);
            } else {
                executiveSummary = StartupExecutiveSummaryResponse.builder()
                        .startupReadinessScore(report.getStartupReadinessScore())
                        .finalRecommendation(report.getFinalRecommendation())
                        .executiveSummary(report.getExecutiveSummary())
                        .build();
            }
        } catch (Exception e) {}

        return StartupFullAnalysisResponse
                .builder()
                .overallScore(report.getOverallScore())
                .investmentScore(report.getInvestmentScore())
                .competitionScore(report.getCompetitionScore())
                .financialScore(report.getFinancialScore())
                .customerScore(report.getCustomerScore())
                .riskScore(report.getRiskScore())
                .productStrategyScore(report.getProductStrategyScore())
                .finalVerdict(report.getFinalVerdict())
                .investorAnalysis(report.getInvestorAnalysis())
                .competitorAnalysis(report.getCompetitorAnalysis())
                .financeAnalysis(report.getFinanceAnalysis())
                .customerAnalysis(report.getCustomerAnalysis())
                .riskAnalysis(report.getRiskAnalysis())
                .productStrategyAnalysis(report.getProductStrategyAnalysis())
                .investorDetails(investorDetails)
                .competitorDetails(competitorDetails)
                .financeDetails(financeDetails)
                .customerDetails(customerDetails)
                .riskDetails(riskDetails)
                .productStrategyDetails(productStrategyDetails)
                .executiveSummary(executiveSummary)
                .build();
    }
}