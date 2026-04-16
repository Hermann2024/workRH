package com.workrh.telework.api.dto;

import java.util.List;

public record TeleworkSocialSecuritySummary(
        int totalRelevantWorkMinutes,
        int sicknessMinutesIncluded,
        int residenceTeleworkMinutes,
        int residenceOtherWorkMinutes,
        int otherForeignWorkMinutes,
        double residenceTeleworkPercent,
        double residenceActivityPercent,
        boolean crossBorderActivityDetected,
        boolean frameworkAgreementApplicable,
        boolean article13Required,
        boolean a1Required,
        String evaluationMode,
        String likelyApplicableLegislationCountryCode,
        String certificatePath,
        int maxRetroactivityMonths,
        int recommendedDeclarationMonths,
        List<String> warnings,
        String explanation
) {
}
