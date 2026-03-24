package com.workrh.reporting.api;

import com.workrh.common.subscription.FeatureCode;
import com.workrh.common.subscription.RequiresFeature;
import com.workrh.reporting.api.dto.DashboardResponse;
import com.workrh.reporting.service.ReportingService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportingController {

    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    @RequiresFeature(FeatureCode.DASHBOARD_ADVANCED)
    public DashboardResponse dashboard(@RequestParam int year, @RequestParam int month) {
        return reportingService.dashboard(year, month);
    }

    @GetMapping(value = "/dashboard/export/csv", produces = "text/csv")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    @RequiresFeature(FeatureCode.EXPORTS)
    public ResponseEntity<byte[]> exportCsv(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("dashboard.csv").build().toString())
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(reportingService.exportCsv(year, month));
    }

    @GetMapping(value = "/dashboard/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    @RequiresFeature(FeatureCode.EXPORTS)
    public ResponseEntity<byte[]> exportPdf(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("dashboard.pdf").build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(reportingService.exportPdf(year, month));
    }

    @GetMapping(value = "/dashboard/export/pdf-placeholder", produces = MediaType.TEXT_PLAIN_VALUE)
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    @RequiresFeature(FeatureCode.EXPORTS)
    public ResponseEntity<byte[]> exportPdfPlaceholder(@RequestParam int year, @RequestParam int month) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("dashboard-placeholder.txt").build().toString())
                .contentType(MediaType.TEXT_PLAIN)
                .body(reportingService.exportPdfPlaceholder(year, month));
    }
}
