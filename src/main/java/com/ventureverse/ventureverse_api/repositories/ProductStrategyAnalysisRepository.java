package com.ventureverse.ventureverse_api.repositories;

import com.ventureverse.ventureverse_api.entities.ProductStrategyAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductStrategyAnalysisRepository
        extends JpaRepository<ProductStrategyAnalysis, Long> {

    List<ProductStrategyAnalysis>
    findByStartupIdOrderByCreatedAtDesc(
            Long startupId
    );
}