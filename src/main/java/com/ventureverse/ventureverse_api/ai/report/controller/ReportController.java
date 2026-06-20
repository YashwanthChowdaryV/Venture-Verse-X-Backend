package com.ventureverse.ventureverse_api.ai.report.controller;

import com.ventureverse.ventureverse_api.ai.report.service.ReportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportExportService reportExportService;

    @GetMapping("/export/{reportId}")
    public ResponseEntity<byte[]> export(
            @PathVariable Long reportId) {

        byte[] pdf = reportExportService.exportReport(
                reportId);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=startup-report.pdf")
                .contentType(
                        MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}