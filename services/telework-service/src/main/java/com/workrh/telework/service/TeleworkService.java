package com.workrh.telework.service;

import com.workrh.common.events.TeleworkDeclaredEvent;
import com.workrh.common.events.ThresholdExceededEvent;
import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.telework.api.dto.TeleworkDeclarationRequest;
import com.workrh.telework.api.dto.TeleworkDeclarationResponse;
import com.workrh.telework.api.dto.TeleworkCompanySummaryResponse;
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
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TeleworkService {

    private final TeleworkDeclarationRepository declarationRepository;
    private final ExclusionPeriodRepository exclusionPeriodRepository;
    private final TeleworkPolicyService teleworkPolicyService;
    private final LuxembourgHolidayService holidayService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TeleworkService(
            TeleworkDeclarationRepository declarationRepository,
            ExclusionPeriodRepository exclusionPeriodRepository,
            TeleworkPolicyService teleworkPolicyService,
            LuxembourgHolidayService holidayService,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.declarationRepository = declarationRepository;
        this.exclusionPeriodRepository = exclusionPeriodRepository;
        this.teleworkPolicyService = teleworkPolicyService;
        this.holidayService = holidayService;
        this.kafkaTemplate = kafkaTemplate;
    }

    public TeleworkDeclarationResponse declare(TeleworkDeclarationRequest request) {
        String tenantId = TenantContext.getTenantId();
        TeleworkPolicy policy = teleworkPolicyService.resolvePolicy(request.countryCode());
        validateWorkDate(tenantId, request.employeeId(), request.workDate());

        int annualUsed = annualUsedDays(tenantId, request.employeeId(), request.workDate().getYear()) + 1;
        int monthUsed = monthUsedDays(tenantId, request.employeeId(), request.workDate().getYear(), request.workDate().getMonthValue()) + 1;
        int weeklyUsed = weeklyUsedDays(tenantId, request.employeeId(), request.workDate()) + 1;
        boolean annualFiscalLimitExceeded = annualUsed > policy.getAnnualFiscalLimitDays();
        boolean weeklyCompanyLimitExceeded = policy.isWeeklyLimitEnabled() && weeklyUsed > policy.getWeeklyCompanyLimitDays();

        TeleworkDeclaration declaration = new TeleworkDeclaration();
        declaration.setTenantId(tenantId);
        declaration.setEmployeeId(request.employeeId());
        declaration.setWorkDate(request.workDate());
        declaration.setCountryCode(policy.getCountryCode());
        declaration.setStatus(TeleworkStatus.DECLARED);
        declaration.setAnnualUsedDays(annualUsed);
        declaration.setMonthUsedDays(monthUsed);
        declaration.setAnnualRemainingDays(Math.max(policy.getAnnualFiscalLimitDays() - annualUsed, 0));

        TeleworkDeclaration saved = declarationRepository.save(declaration);
        kafkaTemplate.send("telework-events", new TeleworkDeclaredEvent(
                tenantId,
                saved.getEmployeeId(),
                saved.getId(),
                saved.getWorkDate(),
                saved.getStatus().name(),
                Instant.now()
        ));

        if (annualFiscalLimitExceeded) {
            kafkaTemplate.send("alert-events", new ThresholdExceededEvent(
                    tenantId,
                    saved.getEmployeeId(),
                    annualUsed,
                    policy.getAnnualFiscalLimitDays(),
                    Instant.now()
            ));
        }

        return toResponse(saved, policy, weeklyUsed, annualFiscalLimitExceeded, weeklyCompanyLimitExceeded);
    }

    public TeleworkSummaryResponse summary(Long employeeId, int year, int month, String countryCode) {
        String tenantId = TenantContext.getTenantId();
        TeleworkPolicy policy = teleworkPolicyService.resolvePolicy(countryCode);
        int annualUsed = annualUsedDays(tenantId, employeeId, year);
        int monthUsed = monthUsedDays(tenantId, employeeId, year, month);
        LocalDate referenceDate = LocalDate.of(year, month, 1);
        int weeklyUsed = weeklyUsedDays(tenantId, employeeId, referenceDate);
        boolean annualFiscalLimitExceeded = annualUsed > policy.getAnnualFiscalLimitDays();
        boolean weeklyCompanyLimitExceeded = policy.isWeeklyLimitEnabled() && weeklyUsed > policy.getWeeklyCompanyLimitDays();
        return new TeleworkSummaryResponse(
                employeeId,
                annualUsed,
                Math.max(policy.getAnnualFiscalLimitDays() - annualUsed, 0),
                monthUsed,
                annualFiscalLimitExceeded,
                new TeleworkPolicySnapshot(
                        policy.getCountryCode(),
                        policy.getAnnualFiscalLimitDays(),
                        Math.max(policy.getAnnualFiscalLimitDays() - annualUsed, 0),
                        policy.getWeeklyCompanyLimitDays(),
                        policy.isWeeklyLimitEnabled(),
                        weeklyUsed,
                        annualFiscalLimitExceeded,
                        weeklyCompanyLimitExceeded
                )
        );
    }

    public List<TeleworkDeclarationResponse> history(Long employeeId) {
        return declarationRepository.findAllByTenantIdAndEmployeeId(TenantContext.getTenantId(), employeeId).stream()
                .map(declaration -> {
                    TeleworkPolicy policy = teleworkPolicyService.resolvePolicy(declaration.getCountryCode());
                    int weeklyUsed = weeklyUsedDays(TenantContext.getTenantId(), employeeId, declaration.getWorkDate());
                    return toResponse(
                            declaration,
                            policy,
                            weeklyUsed,
                            declaration.getAnnualUsedDays() > policy.getAnnualFiscalLimitDays(),
                            policy.isWeeklyLimitEnabled() && weeklyUsed > policy.getWeeklyCompanyLimitDays());
                })
                .toList();
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

    private int annualUsedDays(String tenantId, Long employeeId, int year) {
        return (int) declarationRepository.countByTenantIdAndEmployeeIdAndWorkDateBetween(
                tenantId, employeeId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    private int monthUsedDays(String tenantId, Long employeeId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        return (int) declarationRepository.countByTenantIdAndEmployeeIdAndWorkDateBetween(
                tenantId, employeeId, start, start.withDayOfMonth(start.lengthOfMonth()));
    }

    private int weeklyUsedDays(String tenantId, Long employeeId, LocalDate referenceDate) {
        LocalDate start = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate end = referenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return (int) declarationRepository.countByTenantIdAndEmployeeIdAndWorkDateBetween(tenantId, employeeId, start, end);
    }

    private TeleworkDeclarationResponse toResponse(
            TeleworkDeclaration declaration,
            TeleworkPolicy policy,
            int weeklyUsed,
            boolean annualFiscalLimitExceeded,
            boolean weeklyCompanyLimitExceeded) {
        return new TeleworkDeclarationResponse(
                declaration.getId(),
                declaration.getEmployeeId(),
                declaration.getWorkDate(),
                declaration.getCountryCode(),
                declaration.getStatus(),
                declaration.getMonthUsedDays(),
                declaration.getAnnualUsedDays(),
                declaration.getAnnualRemainingDays(),
                weeklyUsed,
                annualFiscalLimitExceeded,
                weeklyCompanyLimitExceeded
        );
    }
}
