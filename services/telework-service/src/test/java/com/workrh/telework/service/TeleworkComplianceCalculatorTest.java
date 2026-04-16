package com.workrh.telework.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.workrh.telework.domain.TeleworkDeclaration;
import com.workrh.telework.domain.TeleworkPolicy;
import com.workrh.telework.domain.TeleworkStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TeleworkComplianceCalculatorTest {

    private final LuxembourgHolidayService holidayService = new LuxembourgHolidayService();
    private final TeleworkComplianceCalculator calculator = new TeleworkComplianceCalculator(holidayService);

    @Test
    void shouldKeepLuxembourgTaxationAndApplyFrameworkAgreementWhenWithinThreshold() {
        TeleworkPolicy policy = francePolicy();
        List<TeleworkDeclaration> declarations = List.of(
                declaration(LocalDate.of(2026, 3, 2), "FR", 480, 480, 0, 0, null, true),
                declaration(LocalDate.of(2026, 3, 3), "FR", 480, 480, 0, 0, null, true),
                declaration(LocalDate.of(2026, 3, 4), "FR", 480, 480, 0, 0, null, true),
                declaration(LocalDate.of(2026, 3, 5), "FR", 480, 480, 0, 0, null, true),
                declaration(LocalDate.of(2026, 3, 9), "FR", 480, 480, 0, 0, null, true),
                declaration(LocalDate.of(2026, 3, 10), "FR", 480, 480, 0, 0, null, true),
                declaration(LocalDate.of(2026, 3, 11), "FR", 480, 480, 0, 0, null, true),
                declaration(LocalDate.of(2026, 3, 12), "FR", 480, 480, 0, 0, null, true)
        );

        TeleworkComplianceCalculator.TeleworkComplianceResult result = calculator.compute(
                policy,
                2026,
                3,
                LocalDate.of(2026, 3, 31),
                declarations,
                List.of()
        );

        assertThat(result.annualFiscalLimitExceeded()).isFalse();
        assertThat(result.fiscal().luxembourgTaxableSharePercent()).isEqualTo(100.0d);
        assertThat(result.socialSecurity().frameworkAgreementApplicable()).isTrue();
        assertThat(result.socialSecurity().likelyApplicableLegislationCountryCode()).isEqualTo("LU");
    }

    @Test
    void shouldAllocateForeignTaxAndSwitchToArticle13WhenResidenceWorkBecomesSubstantial() {
        TeleworkPolicy policy = francePolicy();
        List<TeleworkDeclaration> declarations = new ArrayList<>();
        LocalDate cursor = LocalDate.of(2026, 2, 16);
        while (declarations.size() < 35) {
            if (cursor.getDayOfWeek().getValue() < 6 && !holidayService.isHoliday(cursor)) {
                declarations.add(declaration(cursor, "FR", 480, 480, 0, 0, null, true));
            }
            cursor = cursor.plusDays(1);
        }

        TeleworkComplianceCalculator.TeleworkComplianceResult result = calculator.compute(
                policy,
                2026,
                3,
                LocalDate.of(2026, 3, 31),
                declarations,
                List.of()
        );

        assertThat(result.annualFiscalLimitExceeded()).isTrue();
        assertThat(result.fiscal().foreignTaxableWorkMinutes()).isGreaterThan(0);
        assertThat(result.socialSecurity().frameworkAgreementApplicable()).isFalse();
        assertThat(result.socialSecurity().evaluationMode()).isEqualTo("ARTICLE_13_RESIDENCE_STATE");
        assertThat(result.socialSecurity().likelyApplicableLegislationCountryCode()).isEqualTo("FR");
    }

    @Test
    void shouldIgnoreShortGermanResidenceActivityForFiscalThresholdButKeepItInSocialSecurityComputation() {
        TeleworkPolicy policy = germanyPolicy();
        List<TeleworkDeclaration> declarations = List.of(
                declaration(LocalDate.of(2026, 3, 2), "DE", 480, 20, 0, 0, null, true)
        );

        TeleworkComplianceCalculator.TeleworkComplianceResult result = calculator.compute(
                policy,
                2026,
                3,
                LocalDate.of(2026, 3, 2),
                declarations,
                List.of()
        );

        assertThat(result.annualThresholdDaysUsed()).isZero();
        assertThat(result.fiscal().toleranceRuleApplied()).isTrue();
        assertThat(result.socialSecurity().residenceTeleworkMinutes()).isEqualTo(20);
    }

    private TeleworkPolicy francePolicy() {
        TeleworkPolicy policy = new TeleworkPolicy();
        policy.setCountryCode("FR");
        policy.setAnnualFiscalLimitDays(34);
        policy.setWeeklyCompanyLimitDays(2);
        policy.setStandardDailyWorkMinutes(480);
        policy.setWeeklyLimitEnabled(true);
        policy.setSocialSecurityStandardThresholdPercent(25);
        policy.setSocialSecurityFrameworkThresholdPercent(49);
        policy.setSocialSecurityFrameworkAgreementEligible(true);
        policy.setPartialDayCountsAsFullDay(true);
        policy.setThresholdProrated(false);
        policy.setThirdCountryDaysCounted(true);
        policy.setTaxRuleLabel("Luxembourg-France : tolerance fiscale de 34 jours");
        return policy;
    }

    private TeleworkPolicy germanyPolicy() {
        TeleworkPolicy policy = francePolicy();
        policy.setCountryCode("DE");
        policy.setShortActivityToleranceMinutes(30);
        return policy;
    }

    private TeleworkDeclaration declaration(
            LocalDate workDate,
            String countryCode,
            int totalWorkMinutes,
            int residenceTeleworkMinutes,
            int residenceNonTeleworkMinutes,
            int otherForeignWorkMinutes,
            String otherForeignCountryCode,
            boolean connectedToEmployerInfrastructure) {
        TeleworkDeclaration declaration = new TeleworkDeclaration();
        declaration.setTenantId("tenant-a");
        declaration.setEmployeeId(1L);
        declaration.setWorkDate(workDate);
        declaration.setCountryCode(countryCode);
        declaration.setStatus(TeleworkStatus.DECLARED);
        declaration.setTotalWorkMinutes(totalWorkMinutes);
        declaration.setResidenceTeleworkMinutes(residenceTeleworkMinutes);
        declaration.setResidenceNonTeleworkMinutes(residenceNonTeleworkMinutes);
        declaration.setOtherForeignWorkMinutes(otherForeignWorkMinutes);
        declaration.setOtherForeignCountryCode(otherForeignCountryCode);
        declaration.setConnectedToEmployerInfrastructure(connectedToEmployerInfrastructure);
        return declaration;
    }
}
