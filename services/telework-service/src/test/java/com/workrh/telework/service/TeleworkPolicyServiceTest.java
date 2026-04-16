package com.workrh.telework.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.workrh.common.tenant.TenantContext;
import com.workrh.telework.repository.TeleworkPolicyRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TeleworkPolicyServiceTest {

    private final TeleworkPolicyRepository teleworkPolicyRepository = Mockito.mock(TeleworkPolicyRepository.class);
    private final TeleworkPolicyService teleworkPolicyService = new TeleworkPolicyService(teleworkPolicyRepository);

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldExposeBuiltInFrancePolicy() {
        TenantContext.setTenantId("demo-lu");
        when(teleworkPolicyRepository.findByTenantIdAndCountryCodeAndActiveTrue("demo-lu", "FR")).thenReturn(Optional.empty());
        when(teleworkPolicyRepository.findByTenantIdAndCountryCodeAndActiveTrue("demo-lu", TeleworkPolicyService.DEFAULT_COUNTRY_CODE))
                .thenReturn(Optional.empty());

        var response = teleworkPolicyService.effective("FR");

        assertThat(response.countryCode()).isEqualTo("FR");
        assertThat(response.annualFiscalLimitDays()).isEqualTo(34);
        assertThat(response.standardDailyWorkMinutes()).isEqualTo(480);
        assertThat(response.socialSecurityStandardThresholdPercent()).isEqualTo(25);
        assertThat(response.socialSecurityFrameworkThresholdPercent()).isEqualTo(49);
    }

    @Test
    void shouldExposeBuiltInGermanyTolerance() {
        TenantContext.setTenantId("demo-lu");
        when(teleworkPolicyRepository.findAllByTenantId("demo-lu")).thenReturn(List.of());

        var responses = teleworkPolicyService.list();
        var germany = responses.stream().filter(policy -> "DE".equals(policy.countryCode())).findFirst().orElseThrow();

        assertThat(germany.annualFiscalLimitDays()).isEqualTo(34);
        assertThat(germany.shortActivityToleranceMinutes()).isEqualTo(30);
        assertThat(germany.thresholdProrated()).isFalse();
    }
}
