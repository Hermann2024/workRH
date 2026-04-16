package com.workrh.reporting.service;

import com.workrh.common.events.TeleworkDeclaredEvent;
import com.workrh.common.events.ThresholdAlertEvent;
import com.workrh.common.events.ThresholdExceededEvent;
import com.workrh.common.tenant.TenantContext;
import com.workrh.reporting.api.dto.DashboardResponse;
import com.workrh.reporting.api.dto.MonthlyStatsResponse;
import com.workrh.reporting.domain.TeleworkMetricSnapshot;
import com.workrh.reporting.repository.TeleworkMetricRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ReportingService {

    private static final int ANNUAL_FISCAL_LIMIT = 34;
    private static final int WEEKLY_COMPANY_LIMIT = 2;

    private final TeleworkMetricRepository teleworkMetricRepository;

    public ReportingService(TeleworkMetricRepository teleworkMetricRepository) {
        this.teleworkMetricRepository = teleworkMetricRepository;
    }

    @KafkaListener(topics = "telework-events", groupId = "reporting-service")
    public void onTeleworkDeclared(TeleworkDeclaredEvent event) {
        LocalDate date = event.workDate();
        TeleworkMetricSnapshot snapshot = teleworkMetricRepository
                .findByTenantIdAndEmployeeIdAndYearAndMonth(event.tenantId(), event.employeeId(), date.getYear(), date.getMonthValue())
                .orElseGet(TeleworkMetricSnapshot::new);

        snapshot.setTenantId(event.tenantId());
        snapshot.setEmployeeId(event.employeeId());
        snapshot.setYear(date.getYear());
        snapshot.setMonth(date.getMonthValue());
        snapshot.setUsedDays(snapshot.getUsedDays() + 1);
        snapshot.setAnnualUsedDays(snapshot.getAnnualUsedDays() + 1);
        snapshot.setAnnualRemainingDays(Math.max(ANNUAL_FISCAL_LIMIT - snapshot.getAnnualUsedDays(), 0));
        snapshot.setWeeklyUsedDays(Math.min(snapshot.getWeeklyUsedDays() + 1, 7));
        snapshot.setAnnualFiscalLimitExceeded(snapshot.getAnnualUsedDays() > ANNUAL_FISCAL_LIMIT);
        snapshot.setWeeklyCompanyLimitExceeded(snapshot.getWeeklyUsedDays() > WEEKLY_COMPANY_LIMIT);
        snapshot.setUpdatedAt(Instant.now());
        teleworkMetricRepository.save(snapshot);
    }

    @KafkaListener(topics = "alert-events", groupId = "reporting-service")
    public void onAlertEvent(Object event) {
        if (event instanceof ThresholdAlertEvent) {
            return;
        }
        if (!(event instanceof ThresholdExceededEvent thresholdExceededEvent)) {
            return;
        }
        List<TeleworkMetricSnapshot> metrics = teleworkMetricRepository.findAllByTenantIdAndEmployeeId(
                thresholdExceededEvent.tenantId(),
                thresholdExceededEvent.employeeId()
        );
        metrics.forEach(metric -> {
            metric.setAnnualFiscalLimitExceeded(true);
            metric.setAnnualUsedDays(Math.max(metric.getAnnualUsedDays(), thresholdExceededEvent.annualUsedDays()));
            metric.setAnnualRemainingDays(Math.max(thresholdExceededEvent.annualLimit() - thresholdExceededEvent.annualUsedDays(), 0));
            metric.setUpdatedAt(Instant.now());
            teleworkMetricRepository.save(metric);
        });
    }

    public DashboardResponse dashboard(int year, int month) {
        List<TeleworkMetricSnapshot> metrics = teleworkMetricRepository.findAllByTenantIdAndYearAndMonth(TenantContext.getTenantId(), year, month);
        int totalUsed = metrics.stream().mapToInt(TeleworkMetricSnapshot::getUsedDays).sum();
        int totalRemaining = metrics.stream().mapToInt(TeleworkMetricSnapshot::getAnnualRemainingDays).sum();
        int fiscalAlerts = (int) metrics.stream().filter(TeleworkMetricSnapshot::isAnnualFiscalLimitExceeded).count();
        int weeklyAlerts = (int) metrics.stream().filter(TeleworkMetricSnapshot::isWeeklyCompanyLimitExceeded).count();
        List<DashboardResponse.EmployeeDashboardItem> employees = metrics.stream()
                .map(metric -> new DashboardResponse.EmployeeDashboardItem(
                        metric.getEmployeeId(),
                        metric.getUsedDays(),
                        metric.getAnnualUsedDays(),
                        metric.getAnnualRemainingDays(),
                        metric.getWeeklyUsedDays(),
                        metric.isAnnualFiscalLimitExceeded(),
                        metric.isWeeklyCompanyLimitExceeded()))
                .toList();
        return new DashboardResponse(metrics.size(), totalUsed, totalRemaining, fiscalAlerts, weeklyAlerts, employees);
    }

    public MonthlyStatsResponse monthlyStats(int year) {
        List<TeleworkMetricSnapshot> metrics = teleworkMetricRepository.findAllByTenantIdAndYearOrderByMonthAsc(TenantContext.getTenantId(), year);
        Map<Integer, List<TeleworkMetricSnapshot>> byMonth = metrics.stream()
                .collect(Collectors.groupingBy(TeleworkMetricSnapshot::getMonth));

        List<MonthlyStatsResponse.MonthlyStatItem> months = java.util.stream.IntStream.rangeClosed(1, 12)
                .mapToObj(month -> {
                    List<TeleworkMetricSnapshot> monthMetrics = byMonth.getOrDefault(month, List.of());
                    int usedDays = monthMetrics.stream().mapToInt(TeleworkMetricSnapshot::getUsedDays).sum();
                    int remainingDays = monthMetrics.stream().mapToInt(TeleworkMetricSnapshot::getAnnualRemainingDays).sum();
                    int fiscalAlerts = (int) monthMetrics.stream().filter(TeleworkMetricSnapshot::isAnnualFiscalLimitExceeded).count();
                    int weeklyAlerts = (int) monthMetrics.stream().filter(TeleworkMetricSnapshot::isWeeklyCompanyLimitExceeded).count();
                    return new MonthlyStatsResponse.MonthlyStatItem(
                            month,
                            usedDays,
                            remainingDays,
                            fiscalAlerts,
                            weeklyAlerts,
                            monthMetrics.size()
                    );
                })
                .sorted(Comparator.comparingInt(MonthlyStatsResponse.MonthlyStatItem::month))
                .toList();

        int trackedEmployees = (int) metrics.stream().map(TeleworkMetricSnapshot::getEmployeeId).distinct().count();
        int peakUsedDays = months.stream().mapToInt(MonthlyStatsResponse.MonthlyStatItem::usedDays).max().orElse(0);
        int totalAlertMonths = (int) months.stream()
                .filter(month -> month.fiscalAlerts() > 0 || month.weeklyAlerts() > 0)
                .count();

        return new MonthlyStatsResponse(year, trackedEmployees, peakUsedDays, totalAlertMonths, months);
    }

    public byte[] exportCsv(int year, int month) {
        List<TeleworkMetricSnapshot> metrics = teleworkMetricRepository.findAllByTenantIdAndYearAndMonth(TenantContext.getTenantId(), year, month);
        StringBuilder csv = new StringBuilder("employeeId,usedDays,annualUsedDays,annualRemainingDays,weeklyUsedDays,annualFiscalLimitExceeded,weeklyCompanyLimitExceeded\n");
        metrics.forEach(metric -> csv.append(metric.getEmployeeId()).append(',')
                .append(metric.getUsedDays()).append(',')
                .append(metric.getAnnualUsedDays()).append(',')
                .append(metric.getAnnualRemainingDays()).append(',')
                .append(metric.getWeeklyUsedDays()).append(',')
                .append(metric.isAnnualFiscalLimitExceeded()).append(',')
                .append(metric.isWeeklyCompanyLimitExceeded()).append('\n'));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportPdf(int year, int month) {
        DashboardResponse dashboard = dashboard(year, month);
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float startX = 50;
                float y = 780;
                float lineHeight = 18;
                PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                writeLine(contentStream, boldFont, 18, startX, y, "WorkRH Dashboard RH");
                y -= 28;
                writeLine(contentStream, regularFont, 11, startX, y, "Periode: %02d/%d".formatted(month, year));
                y -= 24;

                writeLine(contentStream, boldFont, 12, startX, y,
                        "Employes suivis: %d".formatted(dashboard.totalEmployeesTracked()));
                y -= lineHeight;
                writeLine(contentStream, regularFont, 12, startX, y,
                        "Jours utilises: %d".formatted(dashboard.totalUsedDays()));
                y -= lineHeight;
                writeLine(contentStream, regularFont, 12, startX, y,
                        "Jours restants: %d".formatted(dashboard.totalRemainingDays()));
                y -= lineHeight;
                writeLine(contentStream, regularFont, 12, startX, y,
                        "Alertes fiscales: %d".formatted(dashboard.fiscalAlerts()));
                y -= lineHeight;
                writeLine(contentStream, regularFont, 12, startX, y,
                        "Alertes hebdo: %d".formatted(dashboard.weeklyAlerts()));
                y -= 30;

                writeLine(contentStream, boldFont, 12, startX, y,
                        "Employe    Utilises    Annuel    Restants    Hebdo    Fiscal    Hebdo");
                y -= lineHeight;

                for (DashboardResponse.EmployeeDashboardItem employee : dashboard.employees()) {
                    if (y < 70) {
                        break;
                    }
                    String line = "%d    %d    %d    %d    %d    %s    %s".formatted(
                            employee.employeeId(),
                            employee.usedDays(),
                            employee.annualUsedDays(),
                            employee.remainingDays(),
                            employee.weeklyUsedDays(),
                            employee.annualFiscalLimitExceeded() ? "ALERTE" : "OK",
                            employee.weeklyCompanyLimitExceeded() ? "ALERTE" : "OK");
                    writeLine(contentStream, regularFont, 11, startX, y, line);
                    y -= 16;
                }
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate PDF report", exception);
        }
    }

    public byte[] exportPdfPlaceholder(int year, int month) {
        DashboardResponse dashboard = dashboard(year, month);
        String content = """
                WorkRH Dashboard Placeholder
                Period: %02d/%d
                Employees tracked: %d
                Used days: %d
                Remaining days: %d
                Fiscal alerts: %d
                Weekly alerts: %d
                """.formatted(
                month,
                year,
                dashboard.totalEmployeesTracked(),
                dashboard.totalUsedDays(),
                dashboard.totalRemainingDays(),
                dashboard.fiscalAlerts(),
                dashboard.weeklyAlerts()
        );
        return content.getBytes(StandardCharsets.UTF_8);
    }

    private void writeLine(PDPageContentStream contentStream, PDType1Font font, int fontSize, float x, float y, String text)
            throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    }
}
