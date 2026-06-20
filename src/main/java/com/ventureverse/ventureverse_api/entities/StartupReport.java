package com.ventureverse.ventureverse_api.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "startup_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartupReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Scores
     */
    private Integer overallScore;

    private Integer investmentScore;

    private Integer competitionScore;

    private Integer financialScore;

    private Integer customerScore;

    private Integer riskScore;

    private Integer productStrategyScore;

    private Integer startupReadinessScore;

    /*
     * Verdicts
     */
    @Column(columnDefinition = "TEXT")
    private String finalVerdict;

    @Column(columnDefinition = "TEXT")
    private String finalRecommendation;

    /*
     * Executive Summary
     */
    @Column(columnDefinition = "TEXT")
    private String executiveSummary;

    /*
     * Agent Analysis Summaries
     */
    @Column(columnDefinition = "TEXT")
    private String investorAnalysis;

    @Column(columnDefinition = "TEXT")
    private String competitorAnalysis;

    @Column(columnDefinition = "TEXT")
    private String financeAnalysis;

    @Column(columnDefinition = "TEXT")
    private String customerAnalysis;

    @Column(columnDefinition = "TEXT")
    private String riskAnalysis;

    @Column(columnDefinition = "TEXT")
    private String productStrategyAnalysis;

    /*
     * Full Agent Responses (JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String investorDetailsJson;

    @Column(columnDefinition = "TEXT")
    private String competitorDetailsJson;

    @Column(columnDefinition = "TEXT")
    private String financeDetailsJson;

    @Column(columnDefinition = "TEXT")
    private String customerDetailsJson;

    @Column(columnDefinition = "TEXT")
    private String riskDetailsJson;

    @Column(columnDefinition = "TEXT")
    private String productStrategyDetailsJson;

    @Column(columnDefinition = "TEXT")
    private String executiveSummaryJson;

    /*
     * Relationships
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "startup_id")
    private Startup startup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /*
     * Audit
     */
    private LocalDateTime createdAt;
}