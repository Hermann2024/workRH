package com.workrh.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.workrh.common.tenant.TenantContext;
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
}
