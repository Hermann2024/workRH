package com.workrh.telework.service;

import com.workrh.telework.api.dto.TeleworkFiscalSummary;
import com.workrh.telework.api.dto.TeleworkSocialSecuritySummary;
import com.workrh.telework.domain.ExclusionPeriod;
import com.workrh.telework.domain.ExclusionType;
import com.workrh.telework.domain.TeleworkDeclaration;
import com.workrh.telework.domain.TeleworkPolicy;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class TeleworkComplianceCalculator {

    private static final double EPSILON = 0.0001d;
    private static final int MAX_RETROACTIVITY_MONTHS = 3;
    private static final int RECOMMENDED_DECLARATION_MONTHS = 12;

    private final LuxembourgHolidayService holidayService;

    public TeleworkComplianceCalculator(LuxembourgHolidayService holidayService) {
        this.holidayService = holidayService;
    }

    public TeleworkComplianceResult compute(
            TeleworkPolicy policy,
            int year,
            int month,
            LocalDate weeklyReferenceDate,
            List<TeleworkDeclaration> annualDeclarations,
            List<ExclusionPeriod> exclusions) {
        YearMonth requestedMonth = YearMonth.of(year, month);
        int standardDailyWorkMinutes = normalizeStandardDailyWorkMinutes(policy);
        Map<LocalDate, TeleworkDeclaration> declarationsByDate = indexDeclarations(annualDeclarations);
        Map<LocalDate, ExclusionType> exclusionsByDate = indexExclusions(exclusions, year);

        FiscalAccumulator fiscalAccumulator = new FiscalAccumulator();
        SocialSecurityAccumulator socialSecurityAccumulator = new SocialSecurityAccumulator();

        LocalDate annualCursor = LocalDate.of(year, 1, 1);
        LocalDate annualEnd = LocalDate.of(year, 12, 31);
        while (!annualCursor.isAfter(annualEnd)) {
            if (!isBusinessDay(annualCursor)) {
                annualCursor = annualCursor.plusDays(1);
                continue;
            }

            TeleworkDeclaration declaration = declarationsByDate.get(annualCursor);
            ExclusionType exclusionType = exclusionsByDate.get(annualCursor);

            if (declaration != null) {
                DayFootprint footprint = footprintFor(policy, declaration);
                fiscalAccumulator.recordDay(footprint, policy, standardDailyWorkMinutes, annualCursor.getMonthValue() == month);
                if (annualCursor.getYear() == year && annualCursor.getMonthValue() == month) {
                    socialSecurityAccumulator.recordDeclaredDay(footprint);
                }
            } else if (annualCursor.getMonthValue() == month && exclusionType == ExclusionType.SICKNESS) {
                socialSecurityAccumulator.recordSicknessDay(standardDailyWorkMinutes);
            } else if (exclusionType == null) {
                fiscalAccumulator.recordImplicitLuxembourgDay(standardDailyWorkMinutes);
                if (annualCursor.getMonthValue() == month) {
                    socialSecurityAccumulator.recordImplicitLuxembourgDay(standardDailyWorkMinutes);
                }
            }

            annualCursor = annualCursor.plusDays(1);
        }

        int weeklyTeleworkDays = computeWeeklyTeleworkDays(policy, declarationsByDate, weeklyReferenceDate);
        boolean annualFiscalLimitExceeded = fiscalAccumulator.thresholdUnitsUsed > policy.getAnnualFiscalLimitDays() + EPSILON;
        boolean weeklyCompanyLimitExceeded = policy.isWeeklyLimitEnabled() && weeklyTeleworkDays > policy.getWeeklyCompanyLimitDays();

        TeleworkFiscalSummary fiscalSummary = buildFiscalSummary(policy, fiscalAccumulator, annualFiscalLimitExceeded);
        TeleworkSocialSecuritySummary socialSecuritySummary = buildSocialSecuritySummary(policy, requestedMonth, socialSecurityAccumulator);

        return new TeleworkComplianceResult(
                displayUnits(policy, fiscalAccumulator.thresholdUnitsUsed),
                remainingDays(policy, fiscalAccumulator.thresholdUnitsUsed),
                displayUnits(policy, fiscalAccumulator.monthThresholdUnitsUsed),
                weeklyTeleworkDays,
                annualFiscalLimitExceeded,
                weeklyCompanyLimitExceeded,
                fiscalSummary,
                socialSecuritySummary
        );
    }

    private TeleworkFiscalSummary buildFiscalSummary(
            TeleworkPolicy policy,
            FiscalAccumulator accumulator,
            boolean thresholdExceeded) {
        boolean toleranceRuleApplied = usesToleranceRule(policy);
        int totalTrackedWorkMinutes = accumulator.totalTrackedWorkMinutes;
        int luxembourgTaxableWorkMinutes;
        int foreignTaxableWorkMinutes;
        String explanation;

        if (toleranceRuleApplied && !thresholdExceeded) {
            luxembourgTaxableWorkMinutes = totalTrackedWorkMinutes;
            foreignTaxableWorkMinutes = 0;
            explanation = "The annual tolerance threshold is not exceeded, so Luxembourg keeps the full salary taxing right for the tracked work time.";
        } else {
            luxembourgTaxableWorkMinutes = accumulator.luxembourgWorkMinutes;
            foreignTaxableWorkMinutes = accumulator.outsideLuxembourgWorkMinutes;
            explanation = toleranceRuleApplied
                    ? "The annual tolerance threshold is exceeded, so work carried out outside Luxembourg must be allocated outside Luxembourg in proportion to actual work time."
                    : "No frontier tax tolerance is applied, so work carried out outside Luxembourg is allocated outside Luxembourg in proportion to actual work time.";
        }

        return new TeleworkFiscalSummary(
                round(accumulator.thresholdUnitsUsed),
                displayUnits(policy, accumulator.thresholdUnitsUsed),
                remainingDays(policy, accumulator.thresholdUnitsUsed),
                policy.getAnnualFiscalLimitDays(),
                accumulator.residenceTeleworkDays,
                accumulator.residenceOtherWorkDays,
                accumulator.otherForeignWorkDays,
                totalTrackedWorkMinutes,
                accumulator.luxembourgWorkMinutes,
                accumulator.outsideLuxembourgWorkMinutes,
                luxembourgTaxableWorkMinutes,
                foreignTaxableWorkMinutes,
                percent(luxembourgTaxableWorkMinutes, totalTrackedWorkMinutes),
                percent(foreignTaxableWorkMinutes, totalTrackedWorkMinutes),
                thresholdExceeded,
                toleranceRuleApplied,
                policy.isPartialDayCountsAsFullDay(),
                policy.getTaxRuleLabel(),
                explanation
        );
    }

    private TeleworkSocialSecuritySummary buildSocialSecuritySummary(
            TeleworkPolicy policy,
            YearMonth requestedMonth,
            SocialSecurityAccumulator accumulator) {
        int totalRelevantWorkMinutes = accumulator.totalRelevantWorkMinutes;
        double residenceTeleworkPercent = percent(accumulator.residenceTeleworkMinutes, totalRelevantWorkMinutes);
        double residenceActivityPercent = percent(
                accumulator.residenceTeleworkMinutes + accumulator.residenceOtherWorkMinutes,
                totalRelevantWorkMinutes
        );

        boolean crossBorderActivityDetected =
                accumulator.residenceTeleworkMinutesRaw > 0 || accumulator.residenceOtherWorkMinutes > 0 || accumulator.otherForeignWorkMinutes > 0;
        boolean frameworkCountryEligible = policy.isSocialSecurityFrameworkAgreementEligible() && !TeleworkPolicyService.DEFAULT_COUNTRY_CODE.equals(policy.getCountryCode());
        boolean teleworkWithinFrameworkBand =
                residenceTeleworkPercent + EPSILON >= policy.getSocialSecurityStandardThresholdPercent()
                        && residenceTeleworkPercent < policy.getSocialSecurityFrameworkThresholdPercent() + 1.0d;
        boolean frameworkAgreementApplicable =
                crossBorderActivityDetected
                        && frameworkCountryEligible
                        && teleworkWithinFrameworkBand
                        && accumulator.residenceOtherWorkMinutes == 0
                        && accumulator.otherForeignWorkMinutes == 0
                        && accumulator.residenceTeleworkMinutes == accumulator.residenceTeleworkMinutesRaw;

        boolean article13Required = crossBorderActivityDetected && !frameworkAgreementApplicable;
        boolean a1Required = crossBorderActivityDetected;
        String likelyApplicableLegislationCountryCode = !crossBorderActivityDetected
                ? "LU"
                : frameworkAgreementApplicable
                        ? "LU"
                        : residenceActivityPercent + EPSILON >= policy.getSocialSecurityStandardThresholdPercent() ? policy.getCountryCode() : "LU";
        String evaluationMode = !crossBorderActivityDetected
                ? "NO_CROSS_BORDER_ACTIVITY"
                : frameworkAgreementApplicable
                        ? "FRAMEWORK_AGREEMENT"
                        : residenceActivityPercent + EPSILON >= policy.getSocialSecurityStandardThresholdPercent()
                                ? "ARTICLE_13_RESIDENCE_STATE"
                                : "ARTICLE_13_LUXEMBOURG_LIKELY";
        String certificatePath = !a1Required
                ? "NONE"
                : frameworkAgreementApplicable
                        ? "CCSS_FRAMEWORK_AGREEMENT_A1"
                        : "ARTICLE_13_RESIDENCE_STATE_DETERMINATION";

        List<String> warnings = new ArrayList<>();
        if (a1Required) {
            warnings.add("Since July 1, 2024, CCSS retroactivity is limited to 3 months.");
        }
        if (!frameworkCountryEligible && crossBorderActivityDetected) {
            warnings.add("The residence country is not configured as a framework-agreement signatory country.");
        }
        if (accumulator.residenceOtherWorkMinutes > 0) {
            warnings.add("Other habitual work carried out in the state of residence excludes the framework agreement.");
        }
        if (accumulator.otherForeignWorkMinutes > 0) {
            warnings.add("Activity in another country besides Luxembourg and the residence state excludes the framework agreement.");
        }
        if (accumulator.residenceTeleworkMinutes != accumulator.residenceTeleworkMinutesRaw) {
            warnings.add("Only telework connected to the employer IT infrastructure can benefit from the framework agreement.");
        }

        String explanation;
        if (!crossBorderActivityDetected) {
            explanation = "No habitual cross-border activity is recorded for " + requestedMonth + ".";
        } else if (frameworkAgreementApplicable) {
            explanation = "Residence telework stays between 25% and less than 50% of relevant work time, exclusively in the residence state, so a Luxembourg A1 can be requested under the framework agreement.";
        } else if (residenceActivityPercent + EPSILON >= policy.getSocialSecurityStandardThresholdPercent()) {
            explanation = "A substantial part of work is performed in the state of residence. Under Article 13, residence-state legislation is likely and the A1 file must be determined by the residence-state body.";
        } else {
            explanation = "Cross-border activity remains below 25% in the state of residence. Under Article 13, Luxembourg legislation is likely to remain applicable, but the A1 file still follows the pluriactivity procedure.";
        }

        return new TeleworkSocialSecuritySummary(
                totalRelevantWorkMinutes,
                accumulator.sicknessMinutesIncluded,
                accumulator.residenceTeleworkMinutes,
                accumulator.residenceOtherWorkMinutes,
                accumulator.otherForeignWorkMinutes,
                residenceTeleworkPercent,
                residenceActivityPercent,
                crossBorderActivityDetected,
                frameworkAgreementApplicable,
                article13Required,
                a1Required,
                evaluationMode,
                likelyApplicableLegislationCountryCode,
                certificatePath,
                MAX_RETROACTIVITY_MONTHS,
                RECOMMENDED_DECLARATION_MONTHS,
                warnings,
                explanation
        );
    }

    private int computeWeeklyTeleworkDays(
            TeleworkPolicy policy,
            Map<LocalDate, TeleworkDeclaration> declarationsByDate,
            LocalDate weeklyReferenceDate) {
        LocalDate weekStart = weeklyReferenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weeklyReferenceDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        int weeklyTeleworkDays = 0;
        LocalDate cursor = weekStart;
        while (!cursor.isAfter(weekEnd)) {
            TeleworkDeclaration declaration = declarationsByDate.get(cursor);
            if (declaration != null && footprintFor(policy, declaration).residenceTeleworkMinutes > 0) {
                weeklyTeleworkDays++;
            }
            cursor = cursor.plusDays(1);
        }
        return weeklyTeleworkDays;
    }

    private Map<LocalDate, TeleworkDeclaration> indexDeclarations(List<TeleworkDeclaration> declarations) {
        Map<LocalDate, TeleworkDeclaration> indexed = new HashMap<>();
        declarations.stream()
                .sorted(Comparator.comparing(TeleworkDeclaration::getWorkDate))
                .forEach(declaration -> indexed.put(declaration.getWorkDate(), declaration));
        return indexed;
    }

    private Map<LocalDate, ExclusionType> indexExclusions(List<ExclusionPeriod> exclusions, int year) {
        Map<LocalDate, ExclusionType> indexed = new HashMap<>();
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);
        for (ExclusionPeriod exclusion : exclusions) {
            LocalDate cursor = exclusion.getStartDate().isBefore(yearStart) ? yearStart : exclusion.getStartDate();
            LocalDate end = exclusion.getEndDate().isAfter(yearEnd) ? yearEnd : exclusion.getEndDate();
            while (!cursor.isAfter(end)) {
                if (isBusinessDay(cursor)) {
                    indexed.put(cursor, mergeExclusion(indexed.get(cursor), exclusion.getType()));
                }
                cursor = cursor.plusDays(1);
            }
        }
        return indexed;
    }

    private ExclusionType mergeExclusion(ExclusionType current, ExclusionType candidate) {
        if (current == ExclusionType.SICKNESS || candidate == ExclusionType.SICKNESS) {
            return ExclusionType.SICKNESS;
        }
        return candidate;
    }

    public DayFootprint footprintFor(TeleworkPolicy policy, TeleworkDeclaration declaration) {
        int standardDailyWorkMinutes = normalizeStandardDailyWorkMinutes(policy);
        int totalWorkMinutes = declaration.getTotalWorkMinutes() > 0 ? declaration.getTotalWorkMinutes() : standardDailyWorkMinutes;
        int residenceTeleworkMinutes = Math.max(declaration.getResidenceTeleworkMinutes(), 0);
        int residenceNonTeleworkMinutes = Math.max(declaration.getResidenceNonTeleworkMinutes(), 0);
        int otherForeignWorkMinutes = Math.max(declaration.getOtherForeignWorkMinutes(), 0);
        int declaredOutsideMinutes = residenceTeleworkMinutes + residenceNonTeleworkMinutes + otherForeignWorkMinutes;

        if (declaredOutsideMinutes > totalWorkMinutes) {
            totalWorkMinutes = declaredOutsideMinutes;
        }

        int luxembourgWorkMinutes = Math.max(totalWorkMinutes - declaredOutsideMinutes, 0);
        int relevantResidenceOutsideMinutes = residenceTeleworkMinutes + residenceNonTeleworkMinutes;
        boolean countsTowardFiscalThreshold = countsTowardFiscalThreshold(policy, relevantResidenceOutsideMinutes, otherForeignWorkMinutes);
        boolean countsTowardSocialSecurityTelework = residenceTeleworkMinutes > 0 && declaration.isConnectedToEmployerInfrastructure();
        return new DayFootprint(
                totalWorkMinutes,
                luxembourgWorkMinutes,
                residenceTeleworkMinutes,
                residenceNonTeleworkMinutes,
                otherForeignWorkMinutes,
                declaration.getOtherForeignCountryCode(),
                declaration.isConnectedToEmployerInfrastructure(),
                countsTowardFiscalThreshold,
                countsTowardSocialSecurityTelework
        );
    }

    private boolean countsTowardFiscalThreshold(TeleworkPolicy policy, int residenceOutsideMinutes, int otherForeignWorkMinutes) {
        boolean residenceCounts = residenceOutsideMinutes > 0;
        if (policy.getShortActivityToleranceMinutes() > 0
                && residenceOutsideMinutes > 0
                && residenceOutsideMinutes < policy.getShortActivityToleranceMinutes()) {
            residenceCounts = false;
        }

        boolean otherForeignCounts = policy.isThirdCountryDaysCounted() && otherForeignWorkMinutes > 0;
        return residenceCounts || otherForeignCounts;
    }

    private boolean isBusinessDay(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }
        return !holidayService.isHoliday(date);
    }

    private boolean usesToleranceRule(TeleworkPolicy policy) {
        return switch (policy.getCountryCode()) {
            case "FR", "BE", "DE" -> true;
            default -> false;
        };
    }

    private int normalizeStandardDailyWorkMinutes(TeleworkPolicy policy) {
        return policy.getStandardDailyWorkMinutes() > 0 ? policy.getStandardDailyWorkMinutes() : 480;
    }

    private int displayUnits(TeleworkPolicy policy, double thresholdUnits) {
        if (policy.isThresholdProrated()) {
            return (int) Math.round(thresholdUnits);
        }
        return (int) Math.round(thresholdUnits);
    }

    private int remainingDays(TeleworkPolicy policy, double thresholdUnits) {
        return (int) Math.max(Math.floor(policy.getAnnualFiscalLimitDays() - thresholdUnits + EPSILON), 0);
    }

    private double percent(int numerator, int denominator) {
        if (denominator <= 0) {
            return 0.0d;
        }
        return round((numerator * 100.0d) / denominator);
    }

    private double round(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    public record TeleworkComplianceResult(
            int annualThresholdDaysUsed,
            int annualThresholdDaysRemaining,
            int monthThresholdDaysUsed,
            int weeklyTeleworkDays,
            boolean annualFiscalLimitExceeded,
            boolean weeklyCompanyLimitExceeded,
            TeleworkFiscalSummary fiscal,
            TeleworkSocialSecuritySummary socialSecurity
    ) {
    }

    public record DayFootprint(
            int totalWorkMinutes,
            int luxembourgWorkMinutes,
            int residenceTeleworkMinutes,
            int residenceNonTeleworkMinutes,
            int otherForeignWorkMinutes,
            String otherForeignCountryCode,
            boolean connectedToEmployerInfrastructure,
            boolean countsTowardFiscalThreshold,
            boolean countsTowardSocialSecurityTelework
    ) {
    }

    private static final class FiscalAccumulator {
        private double thresholdUnitsUsed;
        private double monthThresholdUnitsUsed;
        private int residenceTeleworkDays;
        private int residenceOtherWorkDays;
        private int otherForeignWorkDays;
        private int totalTrackedWorkMinutes;
        private int luxembourgWorkMinutes;
        private int outsideLuxembourgWorkMinutes;

        private void recordDay(DayFootprint footprint, TeleworkPolicy policy, int standardDailyWorkMinutes, boolean inRequestedMonth) {
            totalTrackedWorkMinutes += footprint.totalWorkMinutes;
            luxembourgWorkMinutes += footprint.luxembourgWorkMinutes;
            outsideLuxembourgWorkMinutes += footprint.residenceTeleworkMinutes + footprint.residenceNonTeleworkMinutes + footprint.otherForeignWorkMinutes;

            if (footprint.residenceTeleworkMinutes > 0) {
                residenceTeleworkDays++;
            }
            if (footprint.residenceNonTeleworkMinutes > 0) {
                residenceOtherWorkDays++;
            }
            if (footprint.otherForeignWorkMinutes > 0) {
                otherForeignWorkDays++;
            }

            double dayUnits = thresholdUnitsForDay(footprint, policy, standardDailyWorkMinutes);
            thresholdUnitsUsed += dayUnits;
            if (inRequestedMonth) {
                monthThresholdUnitsUsed += dayUnits;
            }
        }

        private void recordImplicitLuxembourgDay(int standardDailyWorkMinutes) {
            totalTrackedWorkMinutes += standardDailyWorkMinutes;
            luxembourgWorkMinutes += standardDailyWorkMinutes;
        }

        private double thresholdUnitsForDay(DayFootprint footprint, TeleworkPolicy policy, int standardDailyWorkMinutes) {
            if (!footprint.countsTowardFiscalThreshold) {
                return 0.0d;
            }
            if (policy.isThresholdProrated() && !policy.isPartialDayCountsAsFullDay()) {
                int relevantOutsideMinutes = footprint.residenceTeleworkMinutes + footprint.residenceNonTeleworkMinutes
                        + (policy.isThirdCountryDaysCounted() ? footprint.otherForeignWorkMinutes : 0);
                if (standardDailyWorkMinutes <= 0) {
                    return 1.0d;
                }
                return Math.min(relevantOutsideMinutes / (double) standardDailyWorkMinutes, 1.0d);
            }
            return 1.0d;
        }
    }

    private static final class SocialSecurityAccumulator {
        private int totalRelevantWorkMinutes;
        private int sicknessMinutesIncluded;
        private int residenceTeleworkMinutes;
        private int residenceTeleworkMinutesRaw;
        private int residenceOtherWorkMinutes;
        private int otherForeignWorkMinutes;

        private void recordDeclaredDay(DayFootprint footprint) {
            totalRelevantWorkMinutes += footprint.totalWorkMinutes;
            residenceTeleworkMinutesRaw += footprint.residenceTeleworkMinutes;
            if (footprint.countsTowardSocialSecurityTelework) {
                residenceTeleworkMinutes += footprint.residenceTeleworkMinutes;
            } else {
                residenceOtherWorkMinutes += footprint.residenceTeleworkMinutes;
            }
            residenceOtherWorkMinutes += footprint.residenceNonTeleworkMinutes;
            otherForeignWorkMinutes += footprint.otherForeignWorkMinutes;
        }

        private void recordSicknessDay(int standardDailyWorkMinutes) {
            totalRelevantWorkMinutes += standardDailyWorkMinutes;
            sicknessMinutesIncluded += standardDailyWorkMinutes;
        }

        private void recordImplicitLuxembourgDay(int standardDailyWorkMinutes) {
            totalRelevantWorkMinutes += standardDailyWorkMinutes;
        }
    }
}
