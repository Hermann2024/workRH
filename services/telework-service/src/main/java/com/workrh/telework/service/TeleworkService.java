package com.workrh.telework.service;

import com.workrh.common.events.TeleworkDeclaredEvent;
import com.workrh.common.events.ThresholdAlertEvent;
import com.workrh.common.events.ThresholdExceededEvent;
import com.workrh.common.security.SecurityUtils;
import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.telework.api.dto.TeleworkCompanySummaryResponse;
import com.workrh.telework.api.dto.TeleworkDeclarationRequest;
import com.workrh.telework.api.dto.TeleworkDeclarationResponse;
import com.workrh.telework.api.dto.TeleworkPolicySnapshot;
import com.workrh.telework.api.dto.TeleworkSummaryResponse;
import com.workrh.telework.domain.ExclusionPeriod;
import com.workrh.telework.domain.TeleworkDeclaration;
import com.workrh.telework.domain.TeleworkPolicy;
import com.workrh.telework.domain.TeleworkStatus;
import com.workrh.telework.repository.ExclusionPeriodRepository;
import com.workrh.telework.repository.TeleworkDeclarationRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class TeleworkService {

    private static final Logger log = LoggerFactory.getLogger(TeleworkService.class);

    private final TeleworkDeclarationRepository declarationRepository;
    private final ExclusionPeriodRepository exclusionPeriodRepository;
    private final TeleworkPolicyService teleworkPolicyService;
    private final LuxembourgHolidayService holidayService;
    private final TeleworkComplianceCalculator complianceCalculator;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TeleworkService(
            TeleworkDeclarationRepository declarationRepository,
            ExclusionPeriodRepository exclusionPeriodRepository,
            TeleworkPolicyService teleworkPolicyService,
            LuxembourgHolidayService holidayService,
            TeleworkComplianceCalculator complianceCalculator,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.declarationRepository = declarationRepository;
        this.exclusionPeriodRepository = exclusionPeriodRepository;
        this.teleworkPolicyService = teleworkPolicyService;
        this.holidayService = holidayService;
        this.complianceCalculator = complianceCalculator;
        this.kafkaTemplate = kafkaTemplate;
    }

    public TeleworkDeclarationResponse declare(TeleworkDeclarationRequest request) {
        String tenantId = TenantContext.getTenantId();
        assertCanAccessEmployee(request.employeeId());

        TeleworkPolicy policy = teleworkPolicyService.resolvePolicy(request.countryCode());
        validateWorkDate(tenantId, request.employeeId(), request.workDate());
        List<ExclusionPeriod> exclusions = exclusionPeriodRepository.findAllByTenantIdAndEmployeeId(tenantId, request.employeeId());
        List<TeleworkDeclaration> existingAnnualDeclarations = annualDeclarations(
                tenantId,
                request.employeeId(),
                request.workDate().getYear(),
                policy.getCountryCode()
        );
        TeleworkComplianceCalculator.TeleworkComplianceResult previousResult = complianceCalculator.compute(
                policy,
                request.workDate().getYear(),
                request.workDate().getMonthValue(),
                request.workDate(),
                existingAnnualDeclarations,
                exclusions
        );

        TeleworkDeclaration declaration = buildDeclaration(tenantId, policy, request);
        TeleworkDeclaration saved = declarationRepository.save(declaration);

        List<TeleworkDeclaration> annualDeclarations = new ArrayList<>(existingAnnualDeclarations);
        annualDeclarations.add(saved);
        annualDeclarations.sort(Comparator.comparing(TeleworkDeclaration::getWorkDate));
        TeleworkComplianceCalculator.TeleworkComplianceResult result = complianceCalculator.compute(
                policy,
                saved.getWorkDate().getYear(),
                saved.getWorkDate().getMonthValue(),
                saved.getWorkDate(),
                annualDeclarations,
                exclusions
        );

        saved.setAnnualUsedDays(result.annualThresholdDaysUsed());
        saved.setMonthUsedDays(result.monthThresholdDaysUsed());
        saved.setAnnualRemainingDays(result.annualThresholdDaysRemaining());
        saved = declarationRepository.save(saved);

        publishTeleworkDeclared(saved);
        int previousAlertStage = thresholdAlertStage(previousResult.annualThresholdDaysUsed(), policy.getAnnualFiscalLimitDays());
        int currentAlertStage = thresholdAlertStage(result.annualThresholdDaysUsed(), policy.getAnnualFiscalLimitDays());
        if (currentAlertStage > previousAlertStage) {
            publishThresholdAlert(saved, result.annualThresholdDaysUsed(), policy.getAnnualFiscalLimitDays(), currentAlertStage);
        }
        if (!previousResult.annualFiscalLimitExceeded() && result.annualFiscalLimitExceeded()) {
            publishThresholdExceeded(saved, result.annualThresholdDaysUsed(), policy.getAnnualFiscalLimitDays());
        }

        return toResponse(saved, policy, result);
    }

    public TeleworkSummaryResponse summary(Long employeeId, int year, int month, String countryCode) {
        String tenantId = TenantContext.getTenantId();
        assertCanAccessEmployee(employeeId);

        TeleworkPolicy policy = resolvePolicyForSummary(tenantId, employeeId, year, countryCode);
        List<ExclusionPeriod> exclusions = exclusionPeriodRepository.findAllByTenantIdAndEmployeeId(tenantId, employeeId);
        List<TeleworkDeclaration> annualDeclarations = annualDeclarations(tenantId, employeeId, year, policy.getCountryCode());
        TeleworkComplianceCalculator.TeleworkComplianceResult result = complianceCalculator.compute(
                policy,
                year,
                month,
                resolveWeeklyReferenceDate(year, month),
                annualDeclarations,
                exclusions
        );

        return toSummaryResponse(employeeId, policy, result);
    }

    public List<TeleworkDeclarationResponse> history(Long employeeId) {
        String tenantId = TenantContext.getTenantId();
        assertCanAccessEmployee(employeeId);

        List<TeleworkDeclaration> declarations = declarationRepository.findAllByTenantIdAndEmployeeId(tenantId, employeeId).stream()
                .sorted(Comparator.comparing(TeleworkDeclaration::getWorkDate).reversed())
                .toList();
        List<ExclusionPeriod> exclusions = exclusionPeriodRepository.findAllByTenantIdAndEmployeeId(tenantId, employeeId);

        return declarations.stream()
                .map(declaration -> {
                    TeleworkPolicy policy = teleworkPolicyService.resolvePolicy(declaration.getCountryCode());
                    List<TeleworkDeclaration> annualDeclarations = annualDeclarations(
                            tenantId,
                            employeeId,
                            declaration.getWorkDate().getYear(),
                            policy.getCountryCode()
                    );
                    TeleworkComplianceCalculator.TeleworkComplianceResult result = complianceCalculator.compute(
                            policy,
                            declaration.getWorkDate().getYear(),
                            declaration.getWorkDate().getMonthValue(),
                            declaration.getWorkDate(),
                            annualDeclarations,
                            exclusions
                    );
                    return toResponse(declaration, policy, result);
                })
                .toList();
    }

    public List<TeleworkDeclarationResponse> currentEmployeeHistory() {
        return history(requireCurrentEmployeeId());
    }

    public TeleworkCompanySummaryResponse companySummary(int year, int month, String countryCode) {
        String tenantId = TenantContext.getTenantId();
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        Map<Long, List<TeleworkDeclaration>> byEmployee = declarationRepository.findAllByTenantIdAndWorkDateBetween(tenantId, start, end).stream()
                .collect(Collectors.groupingBy(TeleworkDeclaration::getEmployeeId));

        List<TeleworkSummaryResponse> employees = byEmployee.keySet().stream()
                .sorted()
                .map(employeeId -> summary(employeeId, year, month, countryCode))
                .toList();

        int totalAnnualUsedDays = employees.stream().mapToInt(TeleworkSummaryResponse::annualUsedDays).sum();
        int totalAnnualRemainingDays = employees.stream().mapToInt(TeleworkSummaryResponse::annualRemainingDays).sum();
        int totalEmployeesOverFiscalLimit = (int) employees.stream().filter(TeleworkSummaryResponse::annualFiscalLimitExceeded).count();
        int totalEmployeesOverWeeklyPolicy = (int) employees.stream().filter(summary -> summary.policy().weeklyCompanyLimitExceeded()).count();

        return new TeleworkCompanySummaryResponse(
                employees.size(),
                totalAnnualUsedDays,
                totalAnnualRemainingDays,
                totalEmployeesOverFiscalLimit,
                totalEmployeesOverWeeklyPolicy,
                employees
        );
    }

    private TeleworkDeclaration buildDeclaration(String tenantId, TeleworkPolicy policy, TeleworkDeclarationRequest request) {
        int standardDailyWorkMinutes = policy.getStandardDailyWorkMinutes() > 0 ? policy.getStandardDailyWorkMinutes() : 480;
        int totalWorkMinutes = request.totalWorkMinutes() != null ? request.totalWorkMinutes() : standardDailyWorkMinutes;
        boolean splitProvided =
                request.residenceTeleworkMinutes() != null
                        || request.residenceNonTeleworkMinutes() != null
                        || request.otherForeignWorkMinutes() != null;
        int residenceTeleworkMinutes = request.residenceTeleworkMinutes() != null
                ? request.residenceTeleworkMinutes()
                : (splitProvided ? 0 : totalWorkMinutes);
        int residenceNonTeleworkMinutes = request.residenceNonTeleworkMinutes() != null ? request.residenceNonTeleworkMinutes() : 0;
        int otherForeignWorkMinutes = request.otherForeignWorkMinutes() != null ? request.otherForeignWorkMinutes() : 0;
        boolean connectedToEmployerInfrastructure = request.connectedToEmployerInfrastructure() != null
                ? request.connectedToEmployerInfrastructure()
                : residenceTeleworkMinutes > 0;

        validateWorkBreakdown(totalWorkMinutes, residenceTeleworkMinutes, residenceNonTeleworkMinutes, otherForeignWorkMinutes, request.otherForeignCountryCode());

        TeleworkDeclaration declaration = new TeleworkDeclaration();
        declaration.setTenantId(tenantId);
        declaration.setEmployeeId(request.employeeId());
        declaration.setWorkDate(request.workDate());
        declaration.setCountryCode(policy.getCountryCode());
        declaration.setStatus(TeleworkStatus.DECLARED);
        declaration.setTotalWorkMinutes(totalWorkMinutes);
        declaration.setResidenceTeleworkMinutes(residenceTeleworkMinutes);
        declaration.setResidenceNonTeleworkMinutes(residenceNonTeleworkMinutes);
        declaration.setOtherForeignWorkMinutes(otherForeignWorkMinutes);
        declaration.setOtherForeignCountryCode(normalizeCountryCode(request.otherForeignCountryCode()));
        declaration.setConnectedToEmployerInfrastructure(connectedToEmployerInfrastructure);
        return declaration;
    }

    private void validateWorkBreakdown(
            int totalWorkMinutes,
            int residenceTeleworkMinutes,
            int residenceNonTeleworkMinutes,
            int otherForeignWorkMinutes,
            String otherForeignCountryCode) {
        if (totalWorkMinutes <= 0) {
            throw new BadRequestException("Total work minutes must be greater than zero");
        }

        int outsideMinutes = residenceTeleworkMinutes + residenceNonTeleworkMinutes + otherForeignWorkMinutes;
        if (outsideMinutes <= 0) {
            throw new BadRequestException("At least one minute of work outside Luxembourg must be declared");
        }
        if (outsideMinutes > totalWorkMinutes) {
            throw new BadRequestException("Outside-Luxembourg minutes cannot exceed total work minutes");
        }
        if (otherForeignWorkMinutes > 0 && (otherForeignCountryCode == null || otherForeignCountryCode.isBlank())) {
            throw new BadRequestException("A foreign country code is required when foreign work minutes are declared");
        }
    }

    private void validateWorkDate(String tenantId, Long employeeId, LocalDate workDate) {
        if (declarationRepository.existsByTenantIdAndEmployeeIdAndWorkDate(tenantId, employeeId, workDate)) {
            throw new BadRequestException("Telework already declared for this date");
        }
        if (workDate.getDayOfWeek() == DayOfWeek.SATURDAY || workDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new BadRequestException("Week-end excluded from telework count");
        }
        if (holidayService.isHoliday(workDate)) {
            throw new BadRequestException("Luxembourg holiday excluded from telework count");
        }

        List<ExclusionPeriod> exclusions = exclusionPeriodRepository.findAllByTenantIdAndEmployeeId(tenantId, employeeId);
        boolean excluded = exclusions.stream()
                .anyMatch(period -> !workDate.isBefore(period.getStartDate()) && !workDate.isAfter(period.getEndDate()));
        if (excluded) {
            throw new BadRequestException("Date excluded by approved leave or sickness");
        }
    }

    private TeleworkPolicy resolvePolicyForSummary(String tenantId, Long employeeId, int year, String requestedCountryCode) {
        if (requestedCountryCode != null && !requestedCountryCode.isBlank()) {
            return teleworkPolicyService.resolvePolicy(requestedCountryCode);
        }

        List<TeleworkDeclaration> declarations = declarationRepository.findAllByTenantIdAndEmployeeIdAndWorkDateBetween(
                tenantId,
                employeeId,
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31)
        );
        return declarations.stream()
                .max(Comparator.comparing(TeleworkDeclaration::getWorkDate))
                .map(TeleworkDeclaration::getCountryCode)
                .map(teleworkPolicyService::resolvePolicy)
                .orElseGet(() -> teleworkPolicyService.resolvePolicy(null));
    }

    private List<TeleworkDeclaration> annualDeclarations(String tenantId, Long employeeId, int year, String countryCode) {
        List<TeleworkDeclaration> declarations = declarationRepository.findAllByTenantIdAndEmployeeIdAndWorkDateBetween(
                tenantId,
                employeeId,
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31)
        );
        return declarations.stream()
                .filter(declaration -> declaration.getCountryCode() != null && declaration.getCountryCode().equalsIgnoreCase(countryCode))
                .sorted(Comparator.comparing(TeleworkDeclaration::getWorkDate))
                .toList();
    }

    private LocalDate resolveWeeklyReferenceDate(int year, int month) {
        LocalDate now = LocalDate.now();
        YearMonth requestedMonth = YearMonth.of(year, month);
        if (requestedMonth.equals(YearMonth.from(now))) {
            return now;
        }
        return requestedMonth.atEndOfMonth();
    }

    private TeleworkDeclarationResponse toResponse(
            TeleworkDeclaration declaration,
            TeleworkPolicy policy,
            TeleworkComplianceCalculator.TeleworkComplianceResult result) {
        TeleworkComplianceCalculator.DayFootprint footprint = complianceCalculator.footprintFor(policy, declaration);
        return new TeleworkDeclarationResponse(
                declaration.getId(),
                declaration.getEmployeeId(),
                declaration.getWorkDate(),
                declaration.getCountryCode(),
                declaration.getStatus(),
                footprint.totalWorkMinutes(),
                footprint.residenceTeleworkMinutes(),
                footprint.residenceNonTeleworkMinutes(),
                footprint.otherForeignWorkMinutes(),
                footprint.otherForeignCountryCode(),
                declaration.isConnectedToEmployerInfrastructure(),
                footprint.countsTowardFiscalThreshold(),
                footprint.countsTowardSocialSecurityTelework(),
                result.monthThresholdDaysUsed(),
                result.annualThresholdDaysUsed(),
                result.annualThresholdDaysRemaining(),
                result.weeklyTeleworkDays(),
                result.annualFiscalLimitExceeded(),
                result.weeklyCompanyLimitExceeded()
        );
    }

    private TeleworkSummaryResponse toSummaryResponse(
            Long employeeId,
            TeleworkPolicy policy,
            TeleworkComplianceCalculator.TeleworkComplianceResult result) {
        return new TeleworkSummaryResponse(
                employeeId,
                result.annualThresholdDaysUsed(),
                result.annualThresholdDaysRemaining(),
                result.monthThresholdDaysUsed(),
                result.annualFiscalLimitExceeded(),
                new TeleworkPolicySnapshot(
                        policy.getCountryCode(),
                        policy.getAnnualFiscalLimitDays(),
                        result.annualThresholdDaysRemaining(),
                        policy.getWeeklyCompanyLimitDays(),
                        policy.getStandardDailyWorkMinutes(),
                        policy.getSocialSecurityStandardThresholdPercent(),
                        policy.getSocialSecurityFrameworkThresholdPercent(),
                        policy.getShortActivityToleranceMinutes(),
                        policy.isWeeklyLimitEnabled(),
                        policy.isSocialSecurityFrameworkAgreementEligible(),
                        policy.isPartialDayCountsAsFullDay(),
                        policy.isThresholdProrated(),
                        policy.isThirdCountryDaysCounted(),
                        policy.getTaxRuleLabel(),
                        policy.getLegalReference(),
                        policy.getTaxSourceUrl(),
                        policy.getSocialSecuritySourceUrl(),
                        policy.getNotes(),
                        result.weeklyTeleworkDays(),
                        result.annualFiscalLimitExceeded(),
                        result.weeklyCompanyLimitExceeded()
                ),
                result.fiscal(),
                result.socialSecurity()
        );
    }

    private String normalizeCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return null;
        }
        return countryCode.trim().toUpperCase();
    }

    private void assertCanAccessEmployee(Long employeeId) {
        if (!SecurityUtils.hasAuthority("EMPLOYEE")) {
            return;
        }

        Long currentEmployeeId = requireCurrentEmployeeId();
        if (!currentEmployeeId.equals(employeeId)) {
            throw new AccessDeniedException("Employees can only access their own telework data");
        }
    }

    private Long requireCurrentEmployeeId() {
        Long employeeId = SecurityUtils.currentEmployeeId();
        if (employeeId == null) {
            throw new AccessDeniedException("Missing employee context");
        }
        return employeeId;
    }

    private void publishTeleworkDeclared(TeleworkDeclaration declaration) {
        TeleworkDeclaredEvent event = new TeleworkDeclaredEvent(
                declaration.getTenantId(),
                declaration.getEmployeeId(),
                declaration.getId(),
                declaration.getWorkDate(),
                declaration.getStatus().name(),
                Instant.now()
        );
        publishEvent("telework-events", event, declaration.getTenantId(), declaration.getEmployeeId(), declaration.getId());
    }

    private void publishThresholdExceeded(TeleworkDeclaration declaration, int currentValue, int thresholdValue) {
        ThresholdExceededEvent event = new ThresholdExceededEvent(
                declaration.getTenantId(),
                declaration.getEmployeeId(),
                currentValue,
                thresholdValue,
                Instant.now()
        );
        publishEvent("alert-events", event, declaration.getTenantId(), declaration.getEmployeeId(), declaration.getId());
    }

    private void publishThresholdAlert(
            TeleworkDeclaration declaration,
            int currentValue,
            int thresholdValue,
            int alertStagePercent) {
        ThresholdAlertEvent event = new ThresholdAlertEvent(
                declaration.getTenantId(),
                declaration.getEmployeeId(),
                currentValue,
                thresholdValue,
                alertStagePercent,
                Instant.now()
        );
        publishEvent("alert-events", event, declaration.getTenantId(), declaration.getEmployeeId(), declaration.getId());
    }

    private int thresholdAlertStage(int annualUsedDays, int annualLimit) {
        if (annualLimit <= 0 || annualUsedDays <= 0) {
            return 0;
        }
        int usagePercent = (annualUsedDays * 100) / annualLimit;
        if (usagePercent >= 100 || annualUsedDays >= annualLimit) {
            return 100;
        }
        if (usagePercent >= 90) {
            return 90;
        }
        if (usagePercent >= 80) {
            return 80;
        }
        return 0;
    }

    private void publishEvent(String topic, Object event, String tenantId, Long employeeId, Long declarationId) {
        try {
            kafkaTemplate.send(topic, event)
                    .whenComplete((result, exception) -> {
                        if (exception != null) {
                            log.warn(
                                    "Failed to publish {} event for tenant {} employee {} declaration {}",
                                    topic,
                                    tenantId,
                                    employeeId,
                                    declarationId,
                                    exception
                            );
                        }
                    });
        } catch (RuntimeException exception) {
            log.warn(
                    "Failed to publish {} event for tenant {} employee {} declaration {}",
                    topic,
                    tenantId,
                    employeeId,
                    declarationId,
                    exception
            );
        }
    }
}
