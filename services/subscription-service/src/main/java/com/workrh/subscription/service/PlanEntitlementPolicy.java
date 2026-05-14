package com.workrh.subscription.service;

import com.workrh.common.web.BadRequestException;
import com.workrh.subscription.domain.FeatureCode;
import com.workrh.subscription.domain.PlanCode;
import com.workrh.subscription.domain.TenantSubscription;

final class PlanEntitlementPolicy {

    private PlanEntitlementPolicy() {
    }

    static boolean isPremiumTier(PlanCode planCode) {
        return planCode == PlanCode.PREMIUM || planCode == PlanCode.ENTERPRISE;
    }

    static boolean premiumOptionAllowed(PlanCode planCode, FeatureCode featureCode) {
        return isPremiumTier(planCode) && switch (featureCode) {
            case SMS_NOTIFICATIONS, DECLARATION_AUDIT, ACCOUNTING_EXPORT -> true;
            default -> false;
        };
    }

    static void rejectPremiumOptionsForNonPremiumPlan(
            PlanCode planCode,
            boolean smsOptionEnabled,
            boolean advancedAuditOptionEnabled,
            boolean advancedExportOptionEnabled) {
        if (isPremiumTier(planCode)) {
            return;
        }
        if (smsOptionEnabled || advancedAuditOptionEnabled || advancedExportOptionEnabled) {
            throw new BadRequestException("Premium options require a Premium or Enterprise plan");
        }
    }

    static void clearPremiumOptionsForNonPremiumPlan(TenantSubscription subscription) {
        if (isPremiumTier(subscription.getPlanCode())) {
            return;
        }
        subscription.setSmsOptionEnabled(false);
        subscription.setAdvancedAuditOptionEnabled(false);
        subscription.setAdvancedExportOptionEnabled(false);
    }
}
