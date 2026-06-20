package com.ventureverse.ventureverse_api.repositories;

import com.ventureverse.ventureverse_api.entities.StartupReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StartupReportRepository
        extends JpaRepository<StartupReport, Long> {

    List<StartupReport>
    findByStartupIdOrderByCreatedAtDesc(
            Long startupId
    );
}