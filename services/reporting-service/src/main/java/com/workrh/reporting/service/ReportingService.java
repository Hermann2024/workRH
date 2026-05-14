package com.workrh.reporting.service;

import com.workrh.common.events.TeleworkDeclaredEvent;
import com.workrh.common.events.ThresholdAlertEvent;
import com.workrh.common.events.ThresholdExceededEvent;
import com.workrh.common.tenant.TenantContext;
import com.workrh.reporting.api.dto.DashboardResponse;
import com.workrh.reporting.api.dto.MonthlyStatsResponse;
import com.workrh.reporting.api.dto.TaxSimulationRequest;
import com.workrh.reporting.api.dto.TaxSimulationResponse;
import com.workrh.reporting.domain.TeleworkMetricSnapshot;
import com.workrh.reporting.repository.TeleworkMetricRepository;
import com.workrh.common.web.NotFoundException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class ReportingService {

    private static final int ANNUAL_FISCAL_LIMIT = 34;
    private static final int WEEKLY_COMPANY_LIMIT = 2;
    private static final int RISK_ORANGE_THRESHOLD_PERCENT = 70;
    private static final int RISK_RED_THRESHOLD_PERCENT = 90;
    private static final Color WORKRH_NAVY = new Color(16, 37, 66);
    private static final Color WORKRH_BLUE = new Color(19, 127, 168);
    private static final Color WORKRH_SKY = new Color(235, 247, 252);
    private static final Color WORKRH_RED = new Color(185, 28, 28);
    private static final Color WORKRH_GREEN = new Color(15, 118, 110);
    private static final Color WORKRH_LINE = new Color(210, 220, 232);

    private final TeleworkMetricRepository teleworkMetricRepository;
    private final RestTemplate restTemplate;

    @Value("${user.service.base-url:http://localhost:9081}")
    private String userServiceBaseUrl;

    public ReportingService(TeleworkMetricRepository teleworkMetricRepository, RestTemplate restTemplate) {
        this.teleworkMetricRepository = teleworkMetricRepository;
        this.restTemplate = restTemplate;
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
        snapshot.setAnnualFiscalLimitDays(normalizeAnnualLimit(snapshot.getAnnualFiscalLimitDays()));
        snapshot.setAnnualRemainingDays(Math.max(snapshot.getAnnualFiscalLimitDays() - snapshot.getAnnualUsedDays(), 0));
        snapshot.setWeeklyUsedDays(Math.min(snapshot.getWeeklyUsedDays() + 1, 7));
        snapshot.setAnnualFiscalLimitExceeded(snapshot.getAnnualUsedDays() > snapshot.getAnnualFiscalLimitDays());
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
            metric.setAnnualFiscalLimitDays(normalizeAnnualLimit(thresholdExceededEvent.annualLimit()));
            metric.setAnnualRemainingDays(Math.max(thresholdExceededEvent.annualLimit() - thresholdExceededEvent.annualUsedDays(), 0));
            metric.setUpdatedAt(Instant.now());
            teleworkMetricRepository.save(metric);
        });
    }

    public DashboardResponse dashboard(int year, int month) {
        List<TeleworkMetricSnapshot> metrics = teleworkMetricRepository.findAllByTenantIdAndYearAndMonth(TenantContext.getTenantId(), year, month);
        int totalUsed = metrics.stream().mapToInt(TeleworkMetricSnapshot::getUsedDays).sum();
        int totalRemaining = metrics.stream().mapToInt(TeleworkMetricSnapshot::getAnnualRemainingDays).sum();
        int annualAlerts = (int) metrics.stream().filter(this::isAnnualAlert).count();
        int fiscalAlerts = (int) metrics.stream().filter(TeleworkMetricSnapshot::isAnnualFiscalLimitExceeded).count();
        int weeklyAlerts = (int) metrics.stream().filter(TeleworkMetricSnapshot::isWeeklyCompanyLimitExceeded).count();
        List<DashboardResponse.EmployeeDashboardItem> employees = metrics.stream()
                .map(metric -> new DashboardResponse.EmployeeDashboardItem(
                        metric.getEmployeeId(),
                        metric.getUsedDays(),
                        metric.getAnnualUsedDays(),
                        metric.getAnnualRemainingDays(),
                        metric.getWeeklyUsedDays(),
                        annualAlertLevel(metric),
                        annualAlertLabel(metric),
                        riskScorePercent(metric),
                        riskLevel(metric),
                        riskLabel(metric),
                        metric.isAnnualFiscalLimitExceeded(),
                        metric.isWeeklyCompanyLimitExceeded()))
                .toList();
        return new DashboardResponse(metrics.size(), totalUsed, totalRemaining, annualAlerts, fiscalAlerts, weeklyAlerts, employees);
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

    public TaxSimulationResponse taxSimulation(int year, int month, TaxSimulationRequest request) {
        if (request.annualGrossSalary() == null) {
            throw new IllegalArgumentException("annualGrossSalary is required");
        }

        TeleworkMetricSnapshot metric = teleworkMetricRepository
                .findByTenantIdAndEmployeeIdAndYearAndMonth(TenantContext.getTenantId(), request.employeeId(), year, month)
                .or(() -> teleworkMetricRepository.findTopByTenantIdAndEmployeeIdAndYearAndMonthLessThanEqualOrderByMonthDesc(
                        TenantContext.getTenantId(),
                        request.employeeId(),
                        year,
                        month
                ))
                .orElseThrow(() -> new NotFoundException("No telework reporting metric found for this employee and period"));

        int annualLimit = effectiveAnnualLimit(metric);
        int annualTeleworkDays = metric.getAnnualUsedDays();
        boolean thresholdExceeded = annualTeleworkDays > annualLimit;
        BigDecimal salaryPerWorkDay = request.annualGrossSalary()
                .divide(BigDecimal.valueOf(request.annualContractWorkDays()), 2, RoundingMode.HALF_UP);
        BigDecimal foreignTaxableSalary = thresholdExceeded
                ? salaryPerWorkDay.multiply(BigDecimal.valueOf(annualTeleworkDays)).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal luxembourgTaxableSalary = request.annualGrossSalary()
                .subtract(foreignTaxableSalary)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        return new TaxSimulationResponse(
                request.employeeId(),
                year,
                month,
                annualTeleworkDays,
                annualLimit,
                request.annualContractWorkDays(),
                thresholdExceeded,
                request.annualGrossSalary().setScale(2, RoundingMode.HALF_UP),
                salaryPerWorkDay,
                luxembourgTaxableSalary,
                foreignTaxableSalary,
                thresholdExceeded
                        ? "Threshold exceeded: salary linked to all days worked outside Luxembourg is allocated outside Luxembourg."
                        : "Threshold not exceeded: Luxembourg keeps the taxing right on the tracked annual salary.",
                "Simulation RH. It allocates taxable salary by work location; it is not a final personal income tax assessment."
        );
    }

    public byte[] exportCsv(int year, int month) {
        List<TeleworkMetricSnapshot> metrics = teleworkMetricRepository.findAllByTenantIdAndYearAndMonth(TenantContext.getTenantId(), year, month);
        DashboardResponse dashboard = dashboard(year, month);
        StringBuilder csv = new StringBuilder("\uFEFFsep=;\n");
        csv.append(csvRow("WorkRH - Export reporting télétravail")).append('\n');
        csv.append(csvRow("Tenant", TenantContext.getTenantId())).append('\n');
        csv.append(csvRow("Periode", "%02d/%d".formatted(month, year))).append('\n');
        csv.append(csvRow("Genere le", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))).append('\n');
        csv.append('\n');
        csv.append(csvRow("Synthese")).append('\n');
        csv.append(csvRow("Employes suivis", dashboard.totalEmployeesTracked())).append('\n');
        csv.append(csvRow("Jours utilises", dashboard.totalUsedDays())).append('\n');
        csv.append(csvRow("Jours restants", dashboard.totalRemainingDays())).append('\n');
        csv.append(csvRow("Alertes annuelles", dashboard.annualAlerts())).append('\n');
        csv.append(csvRow("Alertes fiscales", dashboard.fiscalAlerts())).append('\n');
        csv.append(csvRow("Alertes hebdomadaires", dashboard.weeklyAlerts())).append('\n');
        csv.append('\n');
        csv.append(csvRow(
                "Employe",
                "Jours utilises",
                "Cumul annuel",
                "Solde annuel",
                "Alerte annuelle",
                "Score risque",
                "Risque",
                "Hebdomadaire",
                "Statut fiscal",
                "Statut hebdomadaire",
                "Priorite"
        )).append('\n');
        metrics.stream()
                .sorted(Comparator.comparing(TeleworkMetricSnapshot::isAnnualFiscalLimitExceeded).reversed()
                        .thenComparing(Comparator.comparing(TeleworkMetricSnapshot::isWeeklyCompanyLimitExceeded).reversed())
                        .thenComparing(TeleworkMetricSnapshot::getEmployeeId))
                .forEach(metric -> csv.append(csvRow(
                        "#%d".formatted(metric.getEmployeeId()),
                        metric.getUsedDays(),
                        metric.getAnnualUsedDays(),
                        metric.getAnnualRemainingDays(),
                        annualAlertLabel(metric),
                        riskScorePercent(metric) + "%",
                        riskLabel(metric),
                        metric.getWeeklyUsedDays(),
                        metric.isAnnualFiscalLimitExceeded() ? "Alerte fiscale" : "OK",
                        metric.isWeeklyCompanyLimitExceeded() ? "Alerte hebdomadaire" : "OK",
                        priorityLabel(metric)
                )).append('\n'));
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] exportPdf(int year, int month) {
        return exportPdf(year, month, null);
    }

    public byte[] exportPdf(int year, int month, String authorizationHeader) {
        DashboardResponse dashboard = dashboard(year, month);
        Map<Long, String> employeeNames = resolveEmployeeNames(authorizationHeader);
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            float margin = 42;
            float pageWidth = page.getMediaBox().getWidth();
            float y = 788;

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            try {
                y = drawReportHeader(contentStream, boldFont, regularFont, margin, pageWidth, y, year, month);
                y = drawSummaryCards(contentStream, boldFont, regularFont, margin, pageWidth, y, dashboard);
                y -= 18;
                y = drawTableHeader(contentStream, boldFont, margin, y);

                List<DashboardResponse.EmployeeDashboardItem> employees = dashboard.employees().stream()
                        .sorted(Comparator.comparing(DashboardResponse.EmployeeDashboardItem::annualFiscalLimitExceeded).reversed()
                                .thenComparing(Comparator.comparing(DashboardResponse.EmployeeDashboardItem::weeklyCompanyLimitExceeded).reversed())
                                .thenComparing(DashboardResponse.EmployeeDashboardItem::employeeId))
                        .toList();
                int rowIndex = 0;
                for (DashboardResponse.EmployeeDashboardItem employee : employees) {
                    if (y < 78) {
                        drawFooter(contentStream, regularFont, margin, "Suite page suivante");
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        y = 788;
                        y = drawContinuationHeader(contentStream, boldFont, regularFont, margin, pageWidth, y, year, month);
                        y = drawTableHeader(contentStream, boldFont, margin, y);
                    }
                    y = drawEmployeeRow(contentStream, regularFont, margin, y, employee, employeeNames, rowIndex++);
                }

                if (employees.isEmpty()) {
                    drawEmptyState(contentStream, regularFont, margin, y);
                }
                drawFooter(contentStream, regularFont, margin, "Document genere par WorkRH");
            } finally {
                contentStream.close();
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

    private float drawReportHeader(
            PDPageContentStream contentStream,
            PDType1Font boldFont,
            PDType1Font regularFont,
            float margin,
            float pageWidth,
            float y,
            int year,
            int month
    ) throws IOException {
        contentStream.setNonStrokingColor(WORKRH_NAVY);
        contentStream.addRect(0, 735, pageWidth, 107);
        contentStream.fill();

        contentStream.setNonStrokingColor(WORKRH_BLUE);
        contentStream.addRect(0, 735, 8, 107);
        contentStream.fill();

        writeText(contentStream, boldFont, 22, margin, y - 8, "WorkRH Reporting");
            writeText(contentStream, regularFont, 11, margin, y - 30, "Dashboard télétravail et alertes RH");
        drawPill(contentStream, regularFont, pageWidth - 154, y - 22, 112, 22, "Periode %02d/%d".formatted(month, year), WORKRH_SKY, WORKRH_NAVY);
        writeText(contentStream, regularFont, 8, margin, 747, "Tenant: %s  |  Genere le %s".formatted(
                TenantContext.getTenantId(),
                DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        ));
        return 707;
    }

    private float drawContinuationHeader(
            PDPageContentStream contentStream,
            PDType1Font boldFont,
            PDType1Font regularFont,
            float margin,
            float pageWidth,
            float y,
            int year,
            int month
    ) throws IOException {
        contentStream.setNonStrokingColor(WORKRH_NAVY);
        contentStream.addRect(0, 764, pageWidth, 78);
        contentStream.fill();
        writeText(contentStream, boldFont, 18, margin, y - 8, "WorkRH Reporting");
        writeText(contentStream, regularFont, 10, margin, y - 28, "Detail collaborateurs - suite - %02d/%d".formatted(month, year));
        return 730;
    }

    private float drawSummaryCards(
            PDPageContentStream contentStream,
            PDType1Font boldFont,
            PDType1Font regularFont,
            float margin,
            float pageWidth,
            float y,
            DashboardResponse dashboard
    ) throws IOException {
        float gap = 10;
        float cardWidth = (pageWidth - (margin * 2) - (gap * 4)) / 5;
        drawMetricCard(contentStream, boldFont, regularFont, margin, y, cardWidth, "Employes", dashboard.totalEmployeesTracked(), WORKRH_BLUE);
        drawMetricCard(contentStream, boldFont, regularFont, margin + (cardWidth + gap), y, cardWidth, "Jours utilises", dashboard.totalUsedDays(), WORKRH_NAVY);
        drawMetricCard(contentStream, boldFont, regularFont, margin + ((cardWidth + gap) * 2), y, cardWidth, "Alertes annuelles", dashboard.annualAlerts(), dashboard.annualAlerts() > 0 ? WORKRH_RED : WORKRH_GREEN);
        drawMetricCard(contentStream, boldFont, regularFont, margin + ((cardWidth + gap) * 3), y, cardWidth, "Alertes fiscales", dashboard.fiscalAlerts(), dashboard.fiscalAlerts() > 0 ? WORKRH_RED : WORKRH_GREEN);
        drawMetricCard(contentStream, boldFont, regularFont, margin + ((cardWidth + gap) * 4), y, cardWidth, "Alertes hebdo", dashboard.weeklyAlerts(), dashboard.weeklyAlerts() > 0 ? WORKRH_RED : WORKRH_GREEN);
        return y - 92;
    }

    private void drawMetricCard(
            PDPageContentStream contentStream,
            PDType1Font boldFont,
            PDType1Font regularFont,
            float x,
            float y,
            float width,
            String label,
            int value,
            Color accent
    ) throws IOException {
        contentStream.setNonStrokingColor(Color.WHITE);
        contentStream.addRect(x, y - 62, width, 62);
        contentStream.fill();
        contentStream.setStrokingColor(WORKRH_LINE);
        contentStream.addRect(x, y - 62, width, 62);
        contentStream.stroke();
        contentStream.setNonStrokingColor(accent);
        contentStream.addRect(x, y - 62, 4, 62);
        contentStream.fill();
        writeText(contentStream, regularFont, 8, x + 12, y - 18, label.toUpperCase());
        writeText(contentStream, boldFont, 20, x + 12, y - 44, String.valueOf(value));
    }

    private float drawTableHeader(PDPageContentStream contentStream, PDType1Font boldFont, float margin, float y)
            throws IOException {
        contentStream.setNonStrokingColor(WORKRH_SKY);
        contentStream.addRect(margin, y - 24, 511, 24);
        contentStream.fill();
        contentStream.setStrokingColor(WORKRH_LINE);
        contentStream.addRect(margin, y - 24, 511, 24);
        contentStream.stroke();
        writeText(contentStream, boldFont, 8, margin + 10, y - 16, "Employe");
        writeText(contentStream, boldFont, 8, margin + 140, y - 16, "Mois");
        writeText(contentStream, boldFont, 8, margin + 178, y - 16, "Annuel");
        writeText(contentStream, boldFont, 8, margin + 224, y - 16, "Risque");
        writeText(contentStream, boldFont, 8, margin + 286, y - 16, "Hebdo");
        writeText(contentStream, boldFont, 8, margin + 332, y - 16, "Fiscal");
        writeText(contentStream, boldFont, 8, margin + 394, y - 16, "Politique");
        writeText(contentStream, boldFont, 8, margin + 456, y - 16, "Priorite");
        return y - 24;
    }

    private float drawEmployeeRow(
            PDPageContentStream contentStream,
            PDType1Font regularFont,
            float margin,
            float y,
            DashboardResponse.EmployeeDashboardItem employee,
            Map<Long, String> employeeNames,
            int rowIndex
    ) throws IOException {
        float rowHeight = 28;
        contentStream.setNonStrokingColor(rowIndex % 2 == 0 ? Color.WHITE : new Color(248, 251, 253));
        contentStream.addRect(margin, y - rowHeight, 511, rowHeight);
        contentStream.fill();
        contentStream.setStrokingColor(WORKRH_LINE);
        contentStream.moveTo(margin, y - rowHeight);
        contentStream.lineTo(margin + 511, y - rowHeight);
        contentStream.stroke();

        writeText(contentStream, regularFont, 9, margin + 10, y - 18, truncate(employeeNames.getOrDefault(employee.employeeId(), "#%d".formatted(employee.employeeId())), 25));
        writeText(contentStream, regularFont, 9, margin + 146, y - 18, String.valueOf(employee.usedDays()));
        writeText(contentStream, regularFont, 9, margin + 188, y - 18, String.valueOf(employee.annualUsedDays()));
        drawStatus(contentStream, regularFont, margin + 220, y - 22, riskPdfStatus(employee.riskLevel()), "RED".equals(employee.riskLevel()));
        writeText(contentStream, regularFont, 9, margin + 292, y - 18, String.valueOf(employee.weeklyUsedDays()));
        drawStatus(contentStream, regularFont, margin + 328, y - 22, employee.annualFiscalLimitExceeded() ? "ALERTE" : "OK", employee.annualFiscalLimitExceeded());
        drawStatus(contentStream, regularFont, margin + 394, y - 22, employee.weeklyCompanyLimitExceeded() ? "ALERTE" : "OK", employee.weeklyCompanyLimitExceeded());
        writeText(contentStream, regularFont, 8, margin + 456, y - 18, priorityLabel(employee));
        return y - rowHeight;
    }

    private void drawEmptyState(PDPageContentStream contentStream, PDType1Font regularFont, float margin, float y)
            throws IOException {
        contentStream.setNonStrokingColor(new Color(248, 251, 253));
        contentStream.addRect(margin, y - 48, 511, 48);
        contentStream.fill();
        contentStream.setStrokingColor(WORKRH_LINE);
        contentStream.addRect(margin, y - 48, 511, 48);
        contentStream.stroke();
        writeText(contentStream, regularFont, 10, margin + 16, y - 28, "Aucune donnee de reporting pour cette periode.");
    }

    private void drawFooter(PDPageContentStream contentStream, PDType1Font regularFont, float margin, String label)
            throws IOException {
        contentStream.setStrokingColor(WORKRH_LINE);
        contentStream.moveTo(margin, 52);
        contentStream.lineTo(553, 52);
        contentStream.stroke();
        writeText(contentStream, regularFont, 8, margin, 36, label);
        writeText(contentStream, regularFont, 8, 454, 36, "Confidentiel RH");
    }

    private void drawStatus(PDPageContentStream contentStream, PDType1Font regularFont, float x, float y, String label, boolean alert)
            throws IOException {
        drawPill(contentStream, regularFont, x, y, alert ? 58 : 38, 15, label, alert ? new Color(254, 226, 226) : new Color(220, 252, 231), alert ? WORKRH_RED : WORKRH_GREEN);
    }

    private void drawPill(
            PDPageContentStream contentStream,
            PDType1Font regularFont,
            float x,
            float y,
            float width,
            float height,
            String label,
            Color background,
            Color foreground
    ) throws IOException {
        contentStream.setNonStrokingColor(background);
        contentStream.addRect(x, y, width, height);
        contentStream.fill();
        writeText(contentStream, regularFont, 7, x + 7, y + 5, label, foreground);
    }

    private void writeText(PDPageContentStream contentStream, PDType1Font font, int fontSize, float x, float y, String text)
            throws IOException {
        writeText(contentStream, font, fontSize, x, y, text, y < 735 ? WORKRH_NAVY : Color.WHITE);
    }

    private void writeText(PDPageContentStream contentStream, PDType1Font font, int fontSize, float x, float y, String text, Color color)
            throws IOException {
        contentStream.setNonStrokingColor(color);
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(sanitizePdfText(text));
        contentStream.endText();
    }

    private String csvRow(Object... values) {
        return java.util.Arrays.stream(values)
                .map(value -> value == null ? "" : value.toString())
                .map(this::escapeCsv)
                .collect(Collectors.joining(";"));
    }

    private String escapeCsv(String value) {
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(";") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String priorityLabel(TeleworkMetricSnapshot metric) {
        if (metric.isAnnualFiscalLimitExceeded() && metric.isWeeklyCompanyLimitExceeded()) {
            return "Critique";
        }
        if (metric.isAnnualFiscalLimitExceeded() || metric.isWeeklyCompanyLimitExceeded()) {
            return "A surveiller";
        }
        return "Normal";
    }

    private boolean isAnnualAlert(TeleworkMetricSnapshot metric) {
        return riskScorePercent(metric) >= RISK_ORANGE_THRESHOLD_PERCENT;
    }

    private String annualAlertLevel(TeleworkMetricSnapshot metric) {
        int riskScore = riskScorePercent(metric);
        if (riskScore > 100) {
            return "EXCEEDED";
        }
        if (riskScore >= RISK_ORANGE_THRESHOLD_PERCENT) {
            return "WARNING";
        }
        return "OK";
    }

    private String annualAlertLabel(TeleworkMetricSnapshot metric) {
        int annualUsedDays = metric.getAnnualUsedDays();
        int annualLimit = effectiveAnnualLimit(metric);
        int riskScore = riskScorePercent(annualUsedDays, annualLimit);
        if (riskScore > 100) {
        return "Dépassement annuel: %d/%d jours".formatted(annualUsedDays, annualLimit);
        }
        if (riskScore >= RISK_ORANGE_THRESHOLD_PERCENT) {
            return "A surveiller: %d/%d jours".formatted(annualUsedDays, annualLimit);
        }
        return "OK: %d/%d jours".formatted(annualUsedDays, annualLimit);
    }

    private int riskScorePercent(TeleworkMetricSnapshot metric) {
        return riskScorePercent(metric.getAnnualUsedDays(), effectiveAnnualLimit(metric));
    }

    private int effectiveAnnualLimit(TeleworkMetricSnapshot metric) {
        if (metric.getAnnualFiscalLimitDays() > 0) {
            return metric.getAnnualFiscalLimitDays();
        }
        int effectiveLimit = metric.getAnnualUsedDays() + metric.getAnnualRemainingDays();
        if (effectiveLimit <= 0) {
            effectiveLimit = ANNUAL_FISCAL_LIMIT;
        }
        return effectiveLimit;
    }

    private int normalizeAnnualLimit(int annualLimit) {
        return annualLimit > 0 ? annualLimit : ANNUAL_FISCAL_LIMIT;
    }

    private int riskScorePercent(int usedDays, int annualLimit) {
        if (annualLimit <= 0) {
            return usedDays > 0 ? 100 : 0;
        }
        return (int) Math.round((usedDays * 100.0d) / annualLimit);
    }

    private String riskLevel(TeleworkMetricSnapshot metric) {
        return riskLevel(riskScorePercent(metric));
    }

    private String riskLevel(int riskScorePercent) {
        if (riskScorePercent > RISK_RED_THRESHOLD_PERCENT) {
            return "RED";
        }
        if (riskScorePercent >= RISK_ORANGE_THRESHOLD_PERCENT) {
            return "ORANGE";
        }
        return "GREEN";
    }

    private String riskLabel(TeleworkMetricSnapshot metric) {
        return riskLabel(riskScorePercent(metric));
    }

    private String riskLabel(int riskScorePercent) {
        return switch (riskLevel(riskScorePercent)) {
            case "RED" -> "Rouge";
            case "ORANGE" -> "Orange";
            default -> "Vert";
        };
    }

    private String riskPdfStatus(String riskLevel) {
        return switch (riskLevel) {
            case "RED" -> "ROUGE";
            case "ORANGE" -> "ORANGE";
            default -> "VERT";
        };
    }

    private String priorityLabel(DashboardResponse.EmployeeDashboardItem employee) {
        if (employee.annualFiscalLimitExceeded() && employee.weeklyCompanyLimitExceeded()) {
            return "Critique";
        }
        if (employee.annualFiscalLimitExceeded() || employee.weeklyCompanyLimitExceeded()) {
            return "A surveiller";
        }
        return "Normal";
    }

    private Map<Long, String> resolveEmployeeNames(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return Map.of();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        headers.set("X-Tenant-Id", TenantContext.getTenantId());

        try {
            ResponseEntity<List<EmployeeDirectoryItem>> response = restTemplate.exchange(
                    userServiceBaseUrl + "/api/users",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {
                    }
            );
            List<EmployeeDirectoryItem> employees = response.getBody();
            if (employees == null || employees.isEmpty()) {
                return Map.of();
            }

            Map<Long, String> names = new HashMap<>();
            employees.forEach(employee -> names.put(employee.id(), formatEmployeeName(employee)));
            return names;
        } catch (RestClientException exception) {
            return Map.of();
        }
    }

    private String formatEmployeeName(EmployeeDirectoryItem employee) {
        String fullName = "%s %s".formatted(
                employee.firstName() == null ? "" : employee.firstName().trim(),
                employee.lastName() == null ? "" : employee.lastName().trim()
        ).trim();
        return fullName.isBlank() ? "#%d".formatted(employee.id()) : fullName;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(maxLength - 1, 0)) + ".";
    }

    private String sanitizePdfText(String text) {
        return text == null ? "" : text.replace('\u2019', '\'').replace('\u2013', '-').replace('\u2014', '-');
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EmployeeDirectoryItem(Long id, String firstName, String lastName) {
    }
}
