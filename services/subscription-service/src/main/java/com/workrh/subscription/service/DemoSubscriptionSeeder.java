package com.workrh.subscription.service;

import com.workrh.subscription.domain.PlanCode;
import com.workrh.subscription.domain.SubscriptionPlan;
import com.workrh.subscription.domain.SubscriptionStatus;
import com.workrh.subscription.domain.TenantSubscription;
import com.workrh.subscription.repository.SubscriptionPlanRepository;
import com.workrh.subscription.repository.TenantSubscriptionRepository;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class DemoSubscriptionSeeder implements CommandLineRunner {

    private static final String TENANT_ID = "demo-lu";

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;

    public DemoSubscriptionSeeder(
            SubscriptionPlanRepository subscriptionPlanRepository,
            TenantSubscriptionRepository tenantSubscriptionRepository) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
    }

    @Override
    public void run(String... args) {
        SubscriptionPlan plan = subscriptionPlanRepository.findByCode(PlanCode.PRO).orElse(null);
        if (plan == null) {
            return;
        }

        TenantSubscription subscription = tenantSubscriptionRepository.findByTenantId(TENANT_ID)
                .orElseGet(TenantSubscription::new);
        subscription.setTenantId(TENANT_ID);
        subscription.setPlanId(plan.getId());
        subscription.setPlanCode(plan.getCode());
        subscription.setPendingPlanCode(null);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setSeatsPurchased(25);
        subscription.setSmsOptionEnabled(false);
        subscription.setAdvancedAuditOptionEnabled(false);
        subscription.setAdvancedExportOptionEnabled(true);
        subscription.setCancelAtPeriodEnd(false);
        subscription.setCancellationReason(null);
        subscription.setStartsAt(LocalDate.now().minusMonths(1));
        subscription.setRenewsAt(LocalDate.now().plusMonths(1));
        subscription.setCancelledAt(null);
        subscription.setStripeCustomerEmail("rh@company.com");
        subscription.setUpdatedAt(Instant.now());
        if (subscription.getCreatedAt() == null) {
            subscription.setCreatedAt(Instant.now());
        }
        tenantSubscriptionRepository.save(subscription);
    }
}
