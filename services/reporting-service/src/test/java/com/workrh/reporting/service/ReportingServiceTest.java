package com.workrh.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.workrh.common.tenant.TenantContext;
import com.workrh.common.events.ThresholdAlertEvent;
import com.workrh.reporting.api.dto.TaxSimulationRequest;
import com.workrh.reporting.domain.TeleworkMetricSnapshot;
import com.workrh.reporting.repository.TeleworkMetricRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

class ReportingServiceTest {

    private final TeleworkMetricRepository teleworkMetricRepository = Mockito.mock(TeleworkMetricRepository.class);
    private final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    private final ReportingService reportingService = new ReportingService(teleworkMetricRepository, restTemplate);

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
    void shouldExposeRiskScoreOnDashboard() {
        TenantContext.setTenantId("tenant-a");
        TeleworkMetricSnapshot metric = new TeleworkMetricSnapshot();
        metric.setTenantId("tenant-a");
        metric.setEmployeeId(42L);
        metric.setYear(2026);
        metric.setMonth(3);
        metric.setUsedDays(5);
        metric.setAnnualUsedDays(24);
        metric.setAnnualRemainingDays(10);
        metric.setWeeklyUsedDays(1);

        when(teleworkMetricRepository.findAllByTenantIdAndYearAndMonth("tenant-a", 2026, 3))
                .thenReturn(List.of(metric));

        var response = reportingService.dashboard(2026, 3);

        assertThat(response.annualAlerts()).isEqualTo(1);
        assertThat(response.employees()).singleElement()
                .satisfies(employee -> {
                    assertThat(employee.riskScorePercent()).isEqualTo(71);
                    assertThat(employee.riskLevel()).isEqualTo("ORANGE");
                    assertThat(employee.riskLabel()).isEqualTo("Orange");
                });
    }

    @Test
    void shouldSimulateForeignTaxableSalaryWhenThresholdExceeded() {
        TenantContext.setTenantId("tenant-a");
        TeleworkMetricSnapshot metric = new TeleworkMetricSnapshot();
        metric.setTenantId("tenant-a");
        metric.setEmployeeId(42L);
        metric.setYear(2026);
        metric.setMonth(12);
        metric.setAnnualUsedDays(40);
        metric.setAnnualFiscalLimitDays(34);
        metric.setAnnualRemainingDays(0);
        metric.setAnnualFiscalLimitExceeded(true);

        when(teleworkMetricRepository.findByTenantIdAndEmployeeIdAndYearAndMonth("tenant-a", 42L, 2026, 12))
                .thenReturn(java.util.Optional.of(metric));

        var response = reportingService.taxSimulation(
                2026,
                12,
                new TaxSimulationRequest(42L, new BigDecimal("99892.00"), 226)
        );

        assertThat(response.thresholdExceeded()).isTrue();
        assertThat(response.salaryPerWorkDay()).isEqualByComparingTo("442.00");
        assertThat(response.foreignTaxableSalary()).isEqualByComparingTo("17680.00");
        assertThat(response.luxembourgTaxableSalary()).isEqualByComparingTo("82212.00");
    }

    @Test
    void shouldUseLatestAvailableYearMetricForTaxSimulationWhenRequestedMonthHasNoSnapshot() {
        TenantContext.setTenantId("tenant-a");
        TeleworkMetricSnapshot metric = new TeleworkMetricSnapshot();
        metric.setTenantId("tenant-a");
        metric.setEmployeeId(42L);
        metric.setYear(2026);
        metric.setMonth(4);
        metric.setAnnualUsedDays(24);
        metric.setAnnualFiscalLimitDays(34);
        metric.setAnnualRemainingDays(10);

        when(teleworkMetricRepository.findByTenantIdAndEmployeeIdAndYearAndMonth("tenant-a", 42L, 2026, 5))
                .thenReturn(java.util.Optional.empty());
        when(teleworkMetricRepository.findTopByTenantIdAndEmployeeIdAndYearAndMonthLessThanEqualOrderByMonthDesc("tenant-a", 42L, 2026, 5))
                .thenReturn(java.util.Optional.of(metric));

        var response = reportingService.taxSimulation(
                2026,
                5,
                new TaxSimulationRequest(42L, new BigDecimal("60000.00"), 220)
        );

        assertThat(response.month()).isEqualTo(5);
        assertThat(response.annualTeleworkDays()).isEqualTo(24);
        assertThat(response.thresholdExceeded()).isFalse();
        assertThat(response.foreignTaxableSalary()).isEqualByComparingTo("0.00");
    }

    @Test
    void shouldIgnoreThresholdAlertEventsInReportingListener() {
        reportingService.onAlertEvent(new ThresholdAlertEvent("tenant-a", 42L, 28, 34, 80, Instant.now()));
    }
}
