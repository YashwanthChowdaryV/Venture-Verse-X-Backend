package com.ventureverse.ventureverse_api.repositories;

import com.ventureverse.ventureverse_api.entities.CustomerAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAnalysisRepository
        extends JpaRepository<CustomerAnalysis, Long> {
}