package com.workrh.subscription.service;

import com.workrh.subscription.domain.BillingCycle;
import com.workrh.subscription.domain.FeatureCode;
import com.workrh.subscription.domain.PlanCode;
import com.workrh.subscription.domain.SubscriptionPlan;
import com.workrh.subscription.repository.SubscriptionPlanRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionCatalogSeeder implements CommandLineRunner {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    @Value("${stripe.prices.starter:}")
    private String starterStripePriceId;
    @Value("${stripe.prices.pro:}")
    private String proStripePriceId;
    @Value("${stripe.prices.premium:}")
    private String premiumStripePriceId;
    @Value("${stripe.prices.enterprise:}")
    private String enterpriseStripePriceId;

    public SubscriptionCatalogSeeder(SubscriptionPlanRepository subscriptionPlanRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @Override
    public void run(String... args) {
        seed(PlanCode.STARTER, "Starter", new BigDecimal("49.00"), starterStripePriceId, 1, 10, false, false, Set.of(
                FeatureCode.EMPLOYEE_MANAGEMENT,
                FeatureCode.LEAVE_MANAGEMENT,
                FeatureCode.TELEWORK_BASIC,
                FeatureCode.DASHBOARD_BASIC,
                FeatureCode.EMAIL_SUPPORT
        ));
        seed(PlanCode.PRO, "Pro", new BigDecimal("99.00"), proStripePriceId, 10, 50, true, false, Set.of(
                FeatureCode.EMPLOYEE_MANAGEMENT,
                FeatureCode.LEAVE_MANAGEMENT,
                FeatureCode.TELEWORK_BASIC,
                FeatureCode.DASHBOARD_BASIC,
                FeatureCode.EMAIL_SUPPORT,
                FeatureCode.SICKNESS_MANAGEMENT,
                FeatureCode.TELEWORK_COMPLIANCE_34,
                FeatureCode.AUTO_EXCLUSION,
                FeatureCode.THRESHOLD_ALERTS,
                FeatureCode.DASHBOARD_ADVANCED,
                FeatureCode.MONTHLY_STATS,
                FeatureCode.EXPORTS,
                FeatureCode.EMAIL_NOTIFICATIONS,
                FeatureCode.PRIORITY_SUPPORT
        ));
        seed(PlanCode.PREMIUM, "Premium", new BigDecimal("199.00"), premiumStripePriceId, 50, null, false, false, Set.of(
                FeatureCode.EMPLOYEE_MANAGEMENT,
                FeatureCode.LEAVE_MANAGEMENT,
                FeatureCode.TELEWORK_BASIC,
                FeatureCode.DASHBOARD_BASIC,
                FeatureCode.EMAIL_SUPPORT,
                FeatureCode.SICKNESS_MANAGEMENT,
                FeatureCode.TELEWORK_COMPLIANCE_34,
                FeatureCode.AUTO_EXCLUSION,
                FeatureCode.THRESHOLD_ALERTS,
                FeatureCode.DASHBOARD_ADVANCED,
                FeatureCode.MONTHLY_STATS,
                FeatureCode.EXPORTS,
                FeatureCode.EMAIL_NOTIFICATIONS,
                FeatureCode.PRIORITY_SUPPORT,
                FeatureCode.ADVANCED_RBAC,
                FeatureCode.FULL_REPORTING,
                FeatureCode.DECLARATION_AUDIT,
                FeatureCode.PUBLIC_API,
                FeatureCode.SMS_NOTIFICATIONS,
                FeatureCode.COMPANY_BRANDING,
                FeatureCode.ACCOUNTING_EXPORT,
                FeatureCode.SLA_SUPPORT,
                FeatureCode.ONBOARDING_SUPPORT
        ));
        seed(PlanCode.ENTERPRISE, "Enterprise", BigDecimal.ZERO, enterpriseStripePriceId, null, null, false, true, Set.of(
                FeatureCode.EMPLOYEE_MANAGEMENT,
                FeatureCode.LEAVE_MANAGEMENT,
                FeatureCode.TELEWORK_BASIC,
                FeatureCode.DASHBOARD_BASIC,
                FeatureCode.EMAIL_SUPPORT,
                FeatureCode.SICKNESS_MANAGEMENT,
                FeatureCode.TELEWORK_COMPLIANCE_34,
                FeatureCode.AUTO_EXCLUSION,
                FeatureCode.THRESHOLD_ALERTS,
                FeatureCode.DASHBOARD_ADVANCED,
                FeatureCode.MONTHLY_STATS,
                FeatureCode.EXPORTS,
                FeatureCode.EMAIL_NOTIFICATIONS,
                FeatureCode.PRIORITY_SUPPORT,
                FeatureCode.ADVANCED_RBAC,
                FeatureCode.FULL_REPORTING,
                FeatureCode.DECLARATION_AUDIT,
                FeatureCode.PUBLIC_API,
                FeatureCode.SMS_NOTIFICATIONS,
                FeatureCode.COMPANY_BRANDING,
                FeatureCode.ACCOUNTING_EXPORT,
                FeatureCode.SLA_SUPPORT,
                FeatureCode.ONBOARDING_SUPPORT,
                FeatureCode.MULTI_TENANT_ADVANCED,
                FeatureCode.DEDICATED_HOSTING,
                FeatureCode.HARDENED_SECURITY,
                FeatureCode.SSO,
                FeatureCode.CUSTOM_DEVELOPMENT
        ));
    }

    private void seed(
            PlanCode code,
            String name,
            BigDecimal monthlyPrice,
            String stripePriceId,
            Integer minEmployees,
            Integer maxEmployees,
            boolean recommended,
            boolean customPricing,
            Set<FeatureCode> features) {
        SubscriptionPlan plan = subscriptionPlanRepository.findByCode(code).orElseGet(SubscriptionPlan::new);
        plan.setCode(code);
        plan.setName(name);
        plan.setMonthlyPrice(monthlyPrice);
        plan.setStripePriceId(blankToNull(stripePriceId));
        plan.setBillingCycle(BillingCycle.MONTHLY);
        plan.setMinEmployees(minEmployees);
        plan.setMaxEmployees(maxEmployees);
        plan.setRecommended(recommended);
        plan.setCustomPricing(customPricing);
        plan.setFeatures(features);
        plan.setCreatedAt(Instant.now());
        plan.setUpdatedAt(Instant.now());
        subscriptionPlanRepository.save(plan);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
