package com.ventureverse.ventureverse_api.ai.orchestrator.controller;

import com.ventureverse.ventureverse_api.ai.orchestrator.dto.StartupFullAnalysisResponse;
import com.ventureverse.ventureverse_api.ai.orchestrator.dto.StartupReportHistoryResponse;
import com.ventureverse.ventureverse_api.ai.orchestrator.service.AgentOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orchestrator")
@RequiredArgsConstructor
public class OrchestratorController {

    private final AgentOrchestratorService
            orchestratorService;

    @PostMapping("/{startupId}")
    public StartupFullAnalysisResponse analyze(
            @PathVariable Long startupId) {

        return orchestratorService
                .analyzeStartup(startupId);
    }

    @GetMapping("/history/{startupId}")
    public List<StartupReportHistoryResponse>
    getHistory(
            @PathVariable Long startupId) {

        return orchestratorService
                .getReportHistory(startupId);
    }

    @GetMapping("/report/{reportId}")
    public StartupFullAnalysisResponse
    getReport(
            @PathVariable Long reportId) {

        return orchestratorService
                .getReport(reportId);
    }
}