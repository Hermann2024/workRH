package com.workrh.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.subscription.api.dto.SubscriptionChangeRequest;
import com.workrh.subscription.api.dto.SubscriptionCancelRequest;
import com.workrh.subscription.api.dto.SubscriptionRequest;
import com.workrh.subscription.domain.BillingCycle;
import com.workrh.subscription.domain.FeatureCode;
import com.workrh.subscription.domain.PlanCode;
import com.workrh.subscription.domain.SubscriptionPlan;
import com.workrh.subscription.domain.SubscriptionStatus;
import com.workrh.subscription.domain.TenantSubscription;
import com.workrh.subscription.repository.SubscriptionPlanRepository;
import com.workrh.subscription.repository.TenantSubscriptionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class SubscriptionServiceTest {

    private final SubscriptionPlanRepository planRepository = Mockito.mock(SubscriptionPlanRepository.class);
    private final TenantSubscriptionRepository subscriptionRepository = Mockito.mock(TenantSubscriptionRepository.class);
    private final StripeCheckoutService stripeCheckoutService = Mockito.mock(StripeCheckoutService.class);
    private final SubscriptionService subscriptionService = new SubscriptionService(planRepository, subscriptionRepository, stripeCheckoutService);

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCheckIncludedFeature() {
        TenantContext.setTenantId("demo-lu");
        SubscriptionPlan plan = buildPlan(PlanCode.PRO, 10, 50, Set.of(FeatureCode.TELEWORK_COMPLIANCE_34));
        TenantSubscription subscription = new TenantSubscription();
        subscription.setTenantId("demo-lu");
        subscription.setPlanCode(PlanCode.PRO);
        subscription.setSeatsPurchased(20);

        when(subscriptionRepository.findByTenantId("demo-lu")).thenReturn(Optional.of(subscription));
        when(planRepository.findByCode(PlanCode.PRO)).thenReturn(Optional.of(plan));

        var response = subscriptionService.checkFeature(FeatureCode.TELEWORK_COMPLIANCE_34.name());

        assertThat(response.allowed()).isTrue();
        assertThat(response.planCode()).isEqualTo("PRO");
    }

    @Test
    void shouldGrantAllFeaturesToPreviewUser() {
        TenantContext.setTenantId("demo-lu");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "rh@company.com",
                null,
                java.util.List.of(new SimpleGrantedAuthority("HR"))
        ));
        ReflectionTestUtils.setField(subscriptionService, "previewAllFeaturesEnabled", true);
        ReflectionTestUtils.setField(subscriptionService, "previewAllFeaturesUserEmails", "rh@company.com");

        SubscriptionPlan plan = buildPlan(PlanCode.STARTER, 1, 10, Set.of(FeatureCode.EMPLOYEE_MANAGEMENT));
        TenantSubscription subscription = new TenantSubscription();
        subscription.setId(1L);
        subscription.setTenantId("demo-lu");
        subscription.setPlanCode(PlanCode.STARTER);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setSeatsPurchased(5);
        subscription.setStartsAt(LocalDate.now());
        subscription.setRenewsAt(LocalDate.now().plusMonths(1));

        when(subscriptionRepository.findByTenantId("demo-lu")).thenReturn(Optional.of(subscription));
        when(planRepository.findByCode(PlanCode.STARTER)).thenReturn(Optional.of(plan));

        var featureCheck = subscriptionService.checkFeature(FeatureCode.SSO.name());
        var currentSubscription = subscriptionService.currentSubscription();

        assertThat(featureCheck.allowed()).isTrue();
        assertThat(featureCheck.reason()).contains("Preview override");
        assertThat(currentSubscription.entitlements()).contains(
                FeatureCode.SSO.name(),
                FeatureCode.HARDENED_SECURITY.name(),
                FeatureCode.TELEWORK_COMPLIANCE_34.name()
        );
    }

    @Test
    void shouldRejectSeatCountOutsidePlanBounds() {
        TenantContext.setTenantId("demo-lu");
        SubscriptionPlan plan = buildPlan(PlanCode.STARTER, 1, 10, Set.of(FeatureCode.EMPLOYEE_MANAGEMENT));
        when(planRepository.findByCode(PlanCode.STARTER)).thenReturn(Optional.of(plan));
        when(subscriptionRepository.findByTenantId("demo-lu")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.upsertSubscription(new SubscriptionRequest(
                PlanCode.STARTER,
                SubscriptionStatus.ACTIVE,
                20,
                false,
                false,
                false,
                LocalDate.now(),
                LocalDate.now().plusMonths(1)
        ))).isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldUpgradeSubscription() {
        TenantContext.setTenantId("demo-lu");
        SubscriptionPlan starter = buildPlan(PlanCode.STARTER, 1, 10, Set.of(FeatureCode.EMPLOYEE_MANAGEMENT));
        starter.setMonthlyPrice(new BigDecimal("199.00"));
        SubscriptionPlan pro = buildPlan(PlanCode.PRO, 10, 50, Set.of(FeatureCode.TELEWORK_COMPLIANCE_34));
        pro.setId(2L);
        pro.setMonthlyPrice(new BigDecimal("299.00"));

        TenantSubscription subscription = new TenantSubscription();
        subscription.setTenantId("demo-lu");
        subscription.setPlanId(1L);
        subscription.setPlanCode(PlanCode.STARTER);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setSeatsPurchased(10);

        when(subscriptionRepository.findByTenantId("demo-lu")).thenReturn(Optional.of(subscription));
        when(planRepository.findByCode(PlanCode.STARTER)).thenReturn(Optional.of(starter));
        when(planRepository.findByCode(PlanCode.PRO)).thenReturn(Optional.of(pro));
        doNothing().when(stripeCheckoutService).syncExistingSubscriptionChange(subscription, pro, false, false, true);
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);

        var response = subscriptionService.upgrade(new SubscriptionChangeRequest(
                PlanCode.PRO, 20, false, false, true
        ));

        assertThat(response.planCode()).isEqualTo(PlanCode.PRO);
        assertThat(response.advancedExportOptionEnabled()).isTrue();
    }

    @Test
    void shouldScheduleCancellationAtPeriodEnd() {
        TenantContext.setTenantId("demo-lu");
        SubscriptionPlan pro = buildPlan(PlanCode.PRO, 10, 50, Set.of(FeatureCode.TELEWORK_COMPLIANCE_34));
        TenantSubscription subscription = new TenantSubscription();
        subscription.setTenantId("demo-lu");
        subscription.setPlanId(2L);
        subscription.setPlanCode(PlanCode.PRO);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setSeatsPurchased(20);

        when(subscriptionRepository.findByTenantId("demo-lu")).thenReturn(Optional.of(subscription));
        when(planRepository.findByCode(PlanCode.PRO)).thenReturn(Optional.of(pro));
        doNothing().when(stripeCheckoutService).scheduleCancelAtPeriodEnd(subscription);
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);

        var response = subscriptionService.cancel(new SubscriptionCancelRequest("Budget cut"));

        assertThat(response.cancelAtPeriodEnd()).isTrue();
        assertThat(response.cancellationReason()).isEqualTo("Budget cut");
    }

    @Test
    void shouldReactivateCancellation() {
        TenantContext.setTenantId("demo-lu");
        SubscriptionPlan pro = buildPlan(PlanCode.PRO, 10, 50, Set.of(FeatureCode.TELEWORK_COMPLIANCE_34));
        TenantSubscription subscription = new TenantSubscription();
        subscription.setTenantId("demo-lu");
        subscription.setPlanId(2L);
        subscription.setPlanCode(PlanCode.PRO);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setSeatsPurchased(20);
        subscription.setCancelAtPeriodEnd(true);
        subscription.setCancellationReason("Budget cut");

        when(subscriptionRepository.findByTenantId("demo-lu")).thenReturn(Optional.of(subscription));
        when(planRepository.findByCode(PlanCode.PRO)).thenReturn(Optional.of(pro));
        doNothing().when(stripeCheckoutService).reactivateCancellation(subscription);
        when(subscriptionRepository.save(subscription)).thenReturn(subscription);

        var response = subscriptionService.reactivate();

        assertThat(response.cancelAtPeriodEnd()).isFalse();
        assertThat(response.cancellationReason()).isNull();
    }

    private SubscriptionPlan buildPlan(PlanCode code, Integer minEmployees, Integer maxEmployees, Set<FeatureCode> features) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(1L);
        plan.setCode(code);
        plan.setName(code.name());
        plan.setMonthlyPrice(BigDecimal.TEN);
        plan.setBillingCycle(BillingCycle.MONTHLY);
        plan.setMinEmployees(minEmployees);
        plan.setMaxEmployees(maxEmployees);
        plan.setFeatures(features);
        return plan;
    }
}
