package com.ventureverse.ventureverse_api.ai.orchestrator.service;

import com.ventureverse.ventureverse_api.ai.orchestrator.dto.StartupFullAnalysisResponse;
import com.ventureverse.ventureverse_api.ai.orchestrator.dto.StartupReportHistoryResponse;

import java.util.List;

public interface AgentOrchestratorService {

    StartupFullAnalysisResponse analyzeStartup(
            Long startupId);

    List<StartupReportHistoryResponse>
    getReportHistory(Long startupId);

    StartupFullAnalysisResponse
    getReport(Long reportId);
}