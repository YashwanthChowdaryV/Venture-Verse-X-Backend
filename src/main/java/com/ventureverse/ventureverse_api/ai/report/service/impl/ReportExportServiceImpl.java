package com.ventureverse.ventureverse_api.ai.report.service.impl;

import com.ventureverse.ventureverse_api.ai.report.pdf.PdfGenerator;
import com.ventureverse.ventureverse_api.ai.report.service.ReportExportService;
import com.ventureverse.ventureverse_api.entities.StartupReport;
import com.ventureverse.ventureverse_api.repositories.StartupReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportExportServiceImpl
        implements ReportExportService {

    private final StartupReportRepository startupReportRepository;

    private final PdfGenerator pdfGenerator;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportReport(Long reportId) {

        StartupReport report = startupReportRepository
                .findByIdWithRelations(reportId)
                .orElseThrow(() -> new RuntimeException(
                        "Report not found"));

        return pdfGenerator.generate(report);
    }
}