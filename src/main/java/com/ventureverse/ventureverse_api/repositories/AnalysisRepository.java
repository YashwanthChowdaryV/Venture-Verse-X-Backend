package com.ventureverse.ventureverse_api.repositories;

import com.ventureverse.ventureverse_api.entities.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisRepository
        extends JpaRepository<Analysis, Long> {

    List<Analysis> findByUserId(Long userId);

    List<Analysis> findByStartupId(Long startupId);

    List<Analysis> findByStartupIdOrderByCreatedAtDesc(
            Long startupId
    );
}