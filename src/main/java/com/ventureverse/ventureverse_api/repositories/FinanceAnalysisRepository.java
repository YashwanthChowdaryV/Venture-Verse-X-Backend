package com.ventureverse.ventureverse_api.repositories;

import com.ventureverse.ventureverse_api.entities.FinanceAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FinanceAnalysisRepository
        extends JpaRepository<FinanceAnalysis, Long> {

    List<FinanceAnalysis>
    findByStartupIdOrderByCreatedAtDesc(
            Long startupId
    );
}