package com.workrh.telework.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workrh.common.events.ThresholdAlertEvent;
import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.telework.api.dto.TeleworkDeclarationRequest;
import com.workrh.telework.domain.TeleworkDeclaration;
import com.workrh.telework.domain.TeleworkPolicy;
import com.workrh.telework.repository.ExclusionPeriodRepository;
import com.workrh.telework.repository.TeleworkDeclarationRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

class TeleworkServiceTest {

    private final TeleworkDeclarationRepository declarationRepository = Mockito.mock(TeleworkDeclarationRepository.class);
    private final ExclusionPeriodRepository exclusionPeriodRepository = Mockito.mock(ExclusionPeriodRepository.class);
    private final TeleworkPolicyService teleworkPolicyService = Mockito.mock(TeleworkPolicyService.class);
    private final LuxembourgHolidayService holidayService = new LuxembourgHolidayService();
    private final TeleworkComplianceCalculator complianceCalculator = new TeleworkComplianceCalculator(holidayService);
    private final KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
    private final TeleworkService teleworkService = new TeleworkService(
            declarationRepository, exclusionPeriodRepository, teleworkPolicyService, holidayService, complianceCalculator, kafkaTemplate);

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldRejectHolidayDeclaration() {
        TenantContext.setTenantId("tenant-a");
        TeleworkPolicy policy = new TeleworkPolicy();
        policy.setCountryCode("DEFAULT");
        policy.setAnnualFiscalLimitDays(34);
        policy.setWeeklyCompanyLimitDays(2);
        policy.setWeeklyLimitEnabled(true);
        when(teleworkPolicyService.resolvePolicy(null)).thenReturn(policy);
        when(declarationRepository.existsByTenantIdAndEmployeeIdAndWorkDate("tenant-a", 1L, LocalDate.of(2026, 12, 25))).thenReturn(false);
        when(exclusionPeriodRepository.findAllByTenantIdAndEmployeeId("tenant-a", 1L)).thenReturn(List.of());

        assertThatThrownBy(() -> teleworkService.declare(new TeleworkDeclarationRequest(
                1L,
                LocalDate.of(2026, 12, 25),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        )))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("holiday");
    }

    @Test
    void shouldPublishThresholdAlertWhenCrossingWarningStage() {
        TenantContext.setTenantId("tenant-a");
        TeleworkPolicy policy = new TeleworkPolicy();
        policy.setCountryCode("FR");
        policy.setAnnualFiscalLimitDays(34);
        policy.setWeeklyCompanyLimitDays(2);
        policy.setWeeklyLimitEnabled(true);
        policy.setPartialDayCountsAsFullDay(true);
        policy.setStandardDailyWorkMinutes(480);
        when(teleworkPolicyService.resolvePolicy("FR")).thenReturn(policy);
        when(declarationRepository.existsByTenantIdAndEmployeeIdAndWorkDate("tenant-a", 1L, LocalDate.of(2026, 3, 2))).thenReturn(false);
        when(exclusionPeriodRepository.findAllByTenantIdAndEmployeeId("tenant-a", 1L)).thenReturn(List.of());

        List<TeleworkDeclaration> existingDeclarations = new ArrayList<>();
        LocalDate cursor = LocalDate.of(2026, 1, 2);
        long declarationId = 1L;
        while (existingDeclarations.size() < 33) {
            if (cursor.getDayOfWeek() != DayOfWeek.SATURDAY && cursor.getDayOfWeek() != DayOfWeek.SUNDAY) {
                TeleworkDeclaration declaration = new TeleworkDeclaration();
                declaration.setId(declarationId++);
                declaration.setTenantId("tenant-a");
                declaration.setEmployeeId(1L);
                declaration.setCountryCode("FR");
                declaration.setWorkDate(cursor);
                declaration.setTotalWorkMinutes(480);
                declaration.setResidenceTeleworkMinutes(480);
                declaration.setResidenceNonTeleworkMinutes(0);
                declaration.setOtherForeignWorkMinutes(0);
                declaration.setConnectedToEmployerInfrastructure(true);
                existingDeclarations.add(declaration);
            }
            cursor = cursor.plusDays(1);
        }
        when(declarationRepository.findAllByTenantIdAndEmployeeIdAndWorkDateBetween(
                "tenant-a",
                1L,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31)
        )).thenReturn(existingDeclarations);
        when(declarationRepository.save(any(TeleworkDeclaration.class))).thenAnswer(invocation -> {
            TeleworkDeclaration declaration = invocation.getArgument(0);
            if (declaration.getId() == null) {
                declaration.setId(99L);
            }
            return declaration;
        });
        when(kafkaTemplate.send(any(String.class), any())).thenReturn(CompletableFuture.completedFuture(null));

        teleworkService.declare(new TeleworkDeclarationRequest(
                1L,
                LocalDate.of(2026, 3, 2),
                "FR",
                480,
                480,
                0,
                0,
                null,
                true
        ));

        verify(kafkaTemplate, atLeastOnce()).send(eq("alert-events"), argThat(event -> event instanceof ThresholdAlertEvent));
    }
}
