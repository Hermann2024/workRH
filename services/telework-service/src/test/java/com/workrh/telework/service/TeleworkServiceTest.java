package com.workrh.telework.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.telework.api.dto.TeleworkDeclarationRequest;
import com.workrh.telework.domain.TeleworkPolicy;
import com.workrh.telework.repository.ExclusionPeriodRepository;
import com.workrh.telework.repository.TeleworkDeclarationRepository;
import java.time.LocalDate;
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
    private final KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);
    private final TeleworkService teleworkService = new TeleworkService(
            declarationRepository, exclusionPeriodRepository, teleworkPolicyService, holidayService, kafkaTemplate);

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

        assertThatThrownBy(() -> teleworkService.declare(new TeleworkDeclarationRequest(1L, LocalDate.of(2026, 12, 25), null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("holiday");
    }
}
