package com.workrh.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.workrh.common.tenant.TenantContext;
import com.workrh.common.events.ThresholdAlertEvent;
import com.workrh.reporting.domain.TeleworkMetricSnapshot;
import com.workrh.reporting.repository.TeleworkMetricRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ReportingServiceTest {

    private final TeleworkMetricRepository teleworkMetricRepository = Mockito.mock(TeleworkMetricRepository.class);
    private final ReportingService reportingService = new ReportingService(teleworkMetricRepository);

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldGenerateValidPdf() throws Exception {
        TenantContext.setTenantId("tenant-a");
        TeleworkMetricSnapshot metric = new TeleworkMetricSnapshot();
        metric.setTenantId("tenant-a");
        metric.setEmployeeId(42L);
        metric.setYear(2026);
        metric.setMonth(3);
        metric.setUsedDays(12);
        metric.setAnnualUsedDays(12);
        metric.setAnnualRemainingDays(22);
        metric.setWeeklyUsedDays(2);
        metric.setAnnualFiscalLimitExceeded(false);
        metric.setWeeklyCompanyLimitExceeded(false);
        metric.setUpdatedAt(Instant.now());

        when(teleworkMetricRepository.findAllByTenantIdAndYearAndMonth("tenant-a", 2026, 3)).thenReturn(List.of(metric));

        byte[] pdf = reportingService.exportPdf(2026, 3);

        assertThat(pdf).isNotEmpty();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(1);
        }
    }

    @Test
    void shouldGeneratePlaceholderExport() {
        TenantContext.setTenantId("tenant-a");
        when(teleworkMetricRepository.findAllByTenantIdAndYearAndMonth("tenant-a", 2026, 3)).thenReturn(List.of());

        byte[] placeholder = reportingService.exportPdfPlaceholder(2026, 3);

        assertThat(new String(placeholder, StandardCharsets.UTF_8)).contains("WorkRH Dashboard Placeholder");
    }

    @Test
    void shouldBuildMonthlyStatsForWholeYear() {
        TenantContext.setTenantId("tenant-a");
        TeleworkMetricSnapshot marchMetric = new TeleworkMetricSnapshot();
        marchMetric.setTenantId("tenant-a");
        marchMetric.setEmployeeId(42L);
        marchMetric.setYear(2026);
        marchMetric.setMonth(3);
        marchMetric.setUsedDays(12);
        marchMetric.setAnnualRemainingDays(22);
        marchMetric.setAnnualFiscalLimitExceeded(false);
        marchMetric.setWeeklyCompanyLimitExceeded(true);

        TeleworkMetricSnapshot aprilMetric = new TeleworkMetricSnapshot();
        aprilMetric.setTenantId("tenant-a");
        aprilMetric.setEmployeeId(77L);
        aprilMetric.setYear(2026);
        aprilMetric.setMonth(4);
        aprilMetric.setUsedDays(18);
        aprilMetric.setAnnualRemainingDays(12);
        aprilMetric.setAnnualFiscalLimitExceeded(true);
        aprilMetric.setWeeklyCompanyLimitExceeded(false);

        when(teleworkMetricRepository.findAllByTenantIdAndYearOrderByMonthAsc("tenant-a", 2026))
                .thenReturn(List.of(marchMetric, aprilMetric));

        var response = reportingService.monthlyStats(2026);

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.trackedEmployees()).isEqualTo(2);
        assertThat(response.peakUsedDays()).isEqualTo(18);
        assertThat(response.totalAlertMonths()).isEqualTo(2);
        assertThat(response.months()).hasSize(12);
        assertThat(response.months().get(2).usedDays()).isEqualTo(12);
        assertThat(response.months().get(3).fiscalAlerts()).isEqualTo(1);
    }

    @Test
    void shouldIgnoreThresholdAlertEventsInReportingListener() {
        reportingService.onAlertEvent(new ThresholdAlertEvent("tenant-a", 42L, 28, 34, 80, Instant.now()));
    }
}
