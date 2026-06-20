package com.ventureverse.ventureverse_api.repositories;

import com.ventureverse.ventureverse_api.entities.RiskAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskAnalysisRepository
        extends JpaRepository<RiskAnalysis, Long> {

    List<RiskAnalysis>
    findByStartupIdOrderByCreatedAtDesc(
            Long startupId
    );
}