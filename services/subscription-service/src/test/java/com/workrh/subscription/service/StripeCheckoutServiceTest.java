package com.workrh.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.NotFoundException;
import com.workrh.subscription.domain.BillingCycle;
import com.workrh.subscription.domain.PlanCode;
import com.workrh.subscription.domain.SubscriptionPlan;
import com.workrh.subscription.domain.SubscriptionStatus;
import com.workrh.subscription.domain.TenantSubscription;
import com.workrh.subscription.repository.SubscriptionInvoiceRepository;
import com.workrh.subscription.repository.SubscriptionPlanRepository;
import com.workrh.subscription.repository.TenantSubscriptionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

class StripeCheckoutServiceTest {

    private final SubscriptionPlanRepository planRepository = Mockito.mock(SubscriptionPlanRepository.class);
    private final TenantSubscriptionRepository subscriptionRepository = Mockito.mock(TenantSubscriptionRepository.class);
    private final SubscriptionInvoiceRepository invoiceRepository = Mockito.mock(SubscriptionInvoiceRepository.class);
    private final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    private final StripeWebhookVerifier webhookVerifier = Mockito.mock(StripeWebhookVerifier.class);
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, Object> kafkaTemplate = Mockito.mock(KafkaTemplate.class);

    private final StripeCheckoutService stripeCheckoutService = new StripeCheckoutService(
            planRepository,
            subscriptionRepository,
            invoiceRepository,
            restTemplate,
            new ObjectMapper(),
            webhookVerifier,
            kafkaTemplate
    );

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldConfirmCheckoutSessionAndSyncSubscriptionState() {
        TenantContext.setTenantId("acme");
        ReflectionTestUtils.setField(stripeCheckoutService, "stripeSecretKey", "sk_test_123");
        ReflectionTestUtils.setField(stripeCheckoutService, "stripeBaseUrl", "https://api.stripe.com");

        SubscriptionPlan proPlan = buildPlan(PlanCode.PRO);
        TenantSubscription subscription = new TenantSubscription();
        subscription.setTenantId("acme");
        subscription.setPlanCode(PlanCode.STARTER);
        subscription.setStatus(SubscriptionStatus.TRIAL);

        when(planRepository.findByCode(PlanCode.PRO)).thenReturn(Optional.of(proPlan));
        when(subscriptionRepository.findByTenantId("acme")).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(TenantSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.findByStripeSubscriptionId("sub_123")).thenReturn(Optional.of(subscription));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {
                          "id": "cs_123",
                          "status": "complete",
                          "client_reference_id": "acme",
                          "metadata": {
                            "tenantId": "acme",
                            "planCode": "PRO",
                            "seatsPurchased": "25",
                            "smsOptionEnabled": "false",
                            "advancedAuditOptionEnabled": "false",
                            "advancedExportOptionEnabled": "true"
                          },
                          "customer_details": {
                            "email": "rh@acme.com"
                          },
                          "subscription": {
                            "id": "sub_123",
                            "status": "trialing",
                            "current_period_start": 1761955200,
                            "current_period_end": 1763164800,
                            "cancel_at_period_end": false
                          }
                        }
                        """));

        stripeCheckoutService.confirmCheckoutSession("cs_123");

        assertThat(subscription.getPlanCode()).isEqualTo(PlanCode.PRO);
        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.TRIAL);
        assertThat(subscription.getStripeCheckoutSessionId()).isEqualTo("cs_123");
        assertThat(subscription.getStripeSubscriptionId()).isEqualTo("sub_123");
        assertThat(subscription.isAdvancedExportOptionEnabled()).isTrue();
        assertThat(subscription.getStartsAt()).isEqualTo(LocalDate.of(2025, 11, 1));
        assertThat(subscription.getRenewsAt()).isEqualTo(LocalDate.of(2025, 11, 15));
    }

    @Test
    void shouldRejectCheckoutSessionFromAnotherTenant() {
        TenantContext.setTenantId("acme");
        ReflectionTestUtils.setField(stripeCheckoutService, "stripeSecretKey", "sk_test_123");
        ReflectionTestUtils.setField(stripeCheckoutService, "stripeBaseUrl", "https://api.stripe.com");

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("""
                        {
                          "id": "cs_other",
                          "status": "complete",
                          "client_reference_id": "other-company",
                          "metadata": {
                            "tenantId": "other-company",
                            "planCode": "PRO"
                          }
                        }
                        """));

        assertThatThrownBy(() -> stripeCheckoutService.confirmCheckoutSession("cs_other"))
                .isInstanceOf(NotFoundException.class);
        verify(subscriptionRepository, never()).save(any(TenantSubscription.class));
    }

    private SubscriptionPlan buildPlan(PlanCode code) {
        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setId(2L);
        plan.setCode(code);
        plan.setName(code.name());
        plan.setMonthlyPrice(BigDecimal.valueOf(299));
        plan.setBillingCycle(BillingCycle.MONTHLY);
        return plan;
    }
}
