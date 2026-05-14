package com.workrh.subscription.service;

import com.workrh.subscription.domain.BillingCycle;
import com.workrh.subscription.domain.FeatureCode;
import com.workrh.subscription.domain.PlanCode;
import com.workrh.subscription.domain.SubscriptionPlan;
import com.workrh.subscription.repository.SubscriptionPlanRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(10)
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

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${notification.sms.webhook-url:}")
    private String smsWebhookUrl;

    @Value("${subscription.enterprise.enabled:false}")
    private boolean enterprisePlanEnabled;

    public SubscriptionCatalogSeeder(SubscriptionPlanRepository subscriptionPlanRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
    }

    @Override
    public void run(String... args) {
        seed(PlanCode.STARTER, "Starter", new BigDecimal("199.00"), starterStripePriceId, 1, 10, false, false, true, starterFeatures());
        seed(PlanCode.PRO, "Pro", new BigDecimal("299.00"), proStripePriceId, 11, 50, true, false, true, proFeatures());
        seed(PlanCode.PREMIUM, "Premium", new BigDecimal("399.00"), premiumStripePriceId, 51, null, false, false, true, premiumFeatures());
        seed(
                PlanCode.ENTERPRISE,
                "Enterprise",
                BigDecimal.ZERO,
                enterpriseStripePriceId,
                null,
                null,
                false,
                true,
                false,
                enterpriseFeatures()
        );
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
            boolean active,
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
        plan.setActive(active);
        plan.setFeatures(features);
        plan.setCreatedAt(Instant.now());
        plan.setUpdatedAt(Instant.now());
        subscriptionPlanRepository.save(plan);
    }

    private Set<FeatureCode> starterFeatures() {
        return linkedFeatures(
                FeatureCode.EMPLOYEE_MANAGEMENT,
                FeatureCode.LEAVE_MANAGEMENT,
                FeatureCode.TELEWORK_BASIC,
                FeatureCode.DASHBOARD_BASIC,
                FeatureCode.EMAIL_SUPPORT
        );
    }

    private Set<FeatureCode> proFeatures() {
        return linkedFeatures(
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
        );
    }

    private Set<FeatureCode> premiumFeatures() {
        Set<FeatureCode> features = linkedFeatures(
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
                FeatureCode.DECLARATION_AUDIT,
                FeatureCode.ACCOUNTING_EXPORT,
                FeatureCode.SLA_SUPPORT,
                FeatureCode.ONBOARDING_SUPPORT
        );
        if (isSmsOperational()) {
            features.add(FeatureCode.SMS_NOTIFICATIONS);
        }
        return features;
    }

    private Set<FeatureCode> enterpriseFeatures() {
        Set<FeatureCode> features = new LinkedHashSet<>(premiumFeatures());
        features.add(FeatureCode.PUBLIC_API);
        features.add(FeatureCode.DEDICATED_HOSTING);
        features.add(FeatureCode.HARDENED_SECURITY);
        features.add(FeatureCode.SSO);
        features.add(FeatureCode.CUSTOM_DEVELOPMENT);
        return features;
    }

    private boolean isSmsOperational() {
        return smsEnabled && smsWebhookUrl != null && !smsWebhookUrl.isBlank();
    }

    private Set<FeatureCode> linkedFeatures(FeatureCode... features) {
        LinkedHashSet<FeatureCode> values = new LinkedHashSet<>();
        for (FeatureCode feature : features) {
            values.add(feature);
        }
        return values;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
