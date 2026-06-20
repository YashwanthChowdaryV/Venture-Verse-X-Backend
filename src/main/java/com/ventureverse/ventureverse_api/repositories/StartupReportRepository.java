package com.ventureverse.ventureverse_api.repositories;

import com.ventureverse.ventureverse_api.entities.StartupReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StartupReportRepository
                extends JpaRepository<StartupReport, Long> {

        List<StartupReport> findByStartupIdOrderByCreatedAtDesc(
                        Long startupId);

        @Query("""
                            SELECT sr
                            FROM StartupReport sr
                            LEFT JOIN FETCH sr.startup
                            LEFT JOIN FETCH sr.user
                            WHERE sr.id = :reportId
                        """)
        Optional<StartupReport> findByIdWithRelations(
                        @Param("reportId") Long reportId);
}