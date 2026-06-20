package com.ventureverse.ventureverse_api.repositories;

import com.ventureverse.ventureverse_api.entities.CompetitorAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompetitorAnalysisRepository
        extends JpaRepository<CompetitorAnalysis, Long> {

    List<CompetitorAnalysis>
    findByStartupIdOrderByCreatedAtDesc(
            Long startupId
    );
}