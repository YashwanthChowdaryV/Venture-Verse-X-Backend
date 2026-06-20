package com.ventureverse.ventureverse_api.ai.report.service.impl;

import com.ventureverse.ventureverse_api.ai.report.pdf.PdfGenerator;
import com.ventureverse.ventureverse_api.ai.report.service.ReportExportService;
import com.ventureverse.ventureverse_api.entities.StartupReport;
import com.ventureverse.ventureverse_api.repositories.StartupReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportExportServiceImpl
        implements ReportExportService {

    private final StartupReportRepository startupReportRepository;

    private final PdfGenerator pdfGenerator;

    @Override
    public byte[] exportReport(Long reportId) {

        StartupReport report = startupReportRepository
                .findById(reportId)
                .orElseThrow(() -> new RuntimeException(
                        "Report not found"));

        return pdfGenerator.generate(report);
    }
}