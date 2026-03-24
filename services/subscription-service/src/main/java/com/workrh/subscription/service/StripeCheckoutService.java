package com.workrh.subscription.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.common.web.NotFoundException;
import com.workrh.subscription.api.dto.StripeCheckoutRequest;
import com.workrh.subscription.api.dto.StripeCheckoutResponse;
import com.workrh.subscription.domain.PlanCode;
import com.workrh.subscription.domain.SubscriptionPlan;
import com.workrh.subscription.domain.SubscriptionStatus;
import com.workrh.subscription.domain.TenantSubscription;
import com.workrh.subscription.repository.SubscriptionPlanRepository;
import com.workrh.subscription.repository.TenantSubscriptionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
public class StripeCheckoutService {

    private static final int FREE_TRIAL_DAYS = 14;
    private static final BigDecimal SMS_OPTION_PRICE = new BigDecimal("10.00");
    private static final BigDecimal AUDIT_OPTION_PRICE = new BigDecimal("19.00");
    private static final BigDecimal EXPORT_OPTION_PRICE = new BigDecimal("15.00");

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final StripeWebhookVerifier stripeWebhookVerifier;

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${stripe.base-url:https://api.stripe.com}")
    private String stripeBaseUrl;

    @Value("${stripe.webhook.secret:}")
    private String stripeWebhookSecret;
    @Value("${stripe.prices.sms-option:}")
    private String smsOptionStripePriceId;
    @Value("${stripe.prices.advanced-audit-option:}")
    private String advancedAuditOptionStripePriceId;
    @Value("${stripe.prices.advanced-export-option:}")
    private String advancedExportOptionStripePriceId;

    public StripeCheckoutService(
            SubscriptionPlanRepository subscriptionPlanRepository,
            TenantSubscriptionRepository tenantSubscriptionRepository,
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            StripeWebhookVerifier stripeWebhookVerifier) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.stripeWebhookVerifier = stripeWebhookVerifier;
    }

    public StripeCheckoutResponse createCheckoutSession(StripeCheckoutRequest request) {
        SubscriptionPlan plan = getPlan(request.planCode());
        if (plan.isCustomPricing()) {
            throw new BadRequestException("Enterprise plan requires manual quotation");
        }
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new BadRequestException("Stripe secret key is not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(stripeSecretKey, "");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "subscription");
        form.add("success_url", request.successUrl());
        form.add("cancel_url", request.cancelUrl());
        form.add("payment_method_collection", "always");
        form.add("subscription_data[trial_period_days]", Integer.toString(FREE_TRIAL_DAYS));
        form.add("subscription_data[trial_settings][end_behavior][missing_payment_method]", "cancel");
        form.add("client_reference_id", TenantContext.getTenantId());
        form.add("customer_email", request.customerEmail());
        form.add("metadata[tenantId]", TenantContext.getTenantId());
        form.add("metadata[planCode]", request.planCode().name());
        form.add("metadata[seatsPurchased]", Integer.toString(request.seatsPurchased()));
        form.add("metadata[smsOptionEnabled]", Boolean.toString(request.smsOptionEnabled()));
        form.add("metadata[advancedAuditOptionEnabled]", Boolean.toString(request.advancedAuditOptionEnabled()));
        form.add("metadata[advancedExportOptionEnabled]", Boolean.toString(request.advancedExportOptionEnabled()));
        addPaymentMethods(form, request.paymentMethodTypes());

        addRecurringLineItem(form, 0, "%s plan".formatted(plan.getName()), plan.getMonthlyPrice(), plan.getStripePriceId());
        int lineIndex = 1;
        if (request.smsOptionEnabled()) {
            addRecurringLineItem(form, lineIndex++, "SMS notifications option", SMS_OPTION_PRICE, smsOptionStripePriceId);
        }
        if (request.advancedAuditOptionEnabled()) {
            addRecurringLineItem(form, lineIndex++, "Advanced RH audit option", AUDIT_OPTION_PRICE, advancedAuditOptionStripePriceId);
        }
        if (request.advancedExportOptionEnabled()) {
            addRecurringLineItem(form, lineIndex, "Advanced export option", EXPORT_OPTION_PRICE, advancedExportOptionStripePriceId);
        }

        String response;
        try {
            response = restTemplate.postForObject(
                    stripeBaseUrl + "/v1/checkout/sessions",
                    new HttpEntity<>(form, headers),
                    String.class
            );
        } catch (HttpStatusCodeException exception) {
            throw new BadRequestException(extractStripeErrorMessage(exception));
        }

        try {
            JsonNode json = objectMapper.readTree(response);
            return new StripeCheckoutResponse(json.path("id").asText(), json.path("url").asText());
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse Stripe Checkout response", exception);
        }
    }

    public void handleWebhook(String payload, String stripeSignatureHeader) {
        stripeWebhookVerifier.verify(payload, stripeSignatureHeader, stripeWebhookSecret);
        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.path("type").asText();
            JsonNode object = event.path("data").path("object");

            switch (eventType) {
                case "checkout.session.completed" -> upsertSubscriptionFromCheckout(object);
                case "customer.subscription.updated" -> syncUpdatedSubscription(object);
                case "customer.subscription.deleted" -> markSubscriptionCancelled(object);
                case "invoice.payment_failed" -> markSubscriptionPastDue(object);
                case "invoice.paid" -> markSubscriptionActiveFromInvoice(object);
                default -> {
                }
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to process Stripe webhook", exception);
        }
    }

    public void syncExistingSubscriptionChange(
            TenantSubscription subscription,
            SubscriptionPlan targetPlan,
            boolean smsOptionEnabled,
            boolean advancedAuditOptionEnabled,
            boolean advancedExportOptionEnabled) {
        if (!stripeConfigured() || subscription.getStripeSubscriptionId() == null || subscription.getStripeSubscriptionId().isBlank()) {
            return;
        }

        JsonNode stripeSubscription = fetchStripeSubscription(subscription.getStripeSubscriptionId());
        JsonNode items = stripeSubscription.path("items").path("data");

        HttpHeaders headers = stripeHeaders();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("proration_behavior", "create_prorations");

        int formIndex = 0;
        if (items.isArray() && items.size() > 0) {
            JsonNode mainItem = items.get(0);
            form.add("items[%d][id]".formatted(formIndex), mainItem.path("id").asText());
            addSubscriptionItemPrice(form, formIndex, "%s plan".formatted(targetPlan.getName()), targetPlan.getMonthlyPrice(), targetPlan.getStripePriceId());
            form.add("items[%d][quantity]".formatted(formIndex), "1");
            formIndex++;

            for (int index = 1; index < items.size(); index++) {
                JsonNode existingItem = items.get(index);
                form.add("items[%d][id]".formatted(formIndex), existingItem.path("id").asText());
                form.add("items[%d][deleted]".formatted(formIndex), "true");
                formIndex++;
            }
        }

        formIndex = addOptionalRecurringLineItems(form, formIndex, smsOptionEnabled, advancedAuditOptionEnabled, advancedExportOptionEnabled);
        restTemplate.exchange(
                stripeBaseUrl + "/v1/subscriptions/" + subscription.getStripeSubscriptionId(),
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                String.class
        );
    }

    public void scheduleCancelAtPeriodEnd(TenantSubscription subscription) {
        if (!stripeConfigured() || subscription.getStripeSubscriptionId() == null || subscription.getStripeSubscriptionId().isBlank()) {
            return;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("cancel_at_period_end", "true");
        restTemplate.exchange(
                stripeBaseUrl + "/v1/subscriptions/" + subscription.getStripeSubscriptionId(),
                HttpMethod.POST,
                new HttpEntity<>(form, stripeHeaders()),
                String.class
        );
    }

    public void reactivateCancellation(TenantSubscription subscription) {
        if (!stripeConfigured() || subscription.getStripeSubscriptionId() == null || subscription.getStripeSubscriptionId().isBlank()) {
            return;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("cancel_at_period_end", "false");
        restTemplate.exchange(
                stripeBaseUrl + "/v1/subscriptions/" + subscription.getStripeSubscriptionId(),
                HttpMethod.POST,
                new HttpEntity<>(form, stripeHeaders()),
                String.class
        );
    }

    private void upsertSubscriptionFromCheckout(JsonNode object) {
        String tenantId = object.path("metadata").path("tenantId").asText();
        PlanCode planCode = PlanCode.valueOf(object.path("metadata").path("planCode").asText());
        TenantSubscription subscription = tenantSubscriptionRepository.findByTenantId(tenantId)
                .orElseGet(TenantSubscription::new);
        SubscriptionPlan plan = getPlan(planCode);
        subscription.setTenantId(tenantId);
        subscription.setPlanId(plan.getId());
        subscription.setPlanCode(planCode);
        subscription.setStatus(SubscriptionStatus.TRIAL);
        subscription.setSeatsPurchased(object.path("metadata").path("seatsPurchased").asInt(1));
        subscription.setPendingPlanCode(null);
        subscription.setSmsOptionEnabled(object.path("metadata").path("smsOptionEnabled").asBoolean(false));
        subscription.setAdvancedAuditOptionEnabled(object.path("metadata").path("advancedAuditOptionEnabled").asBoolean(false));
        subscription.setAdvancedExportOptionEnabled(object.path("metadata").path("advancedExportOptionEnabled").asBoolean(false));
        subscription.setStripeCustomerEmail(object.path("customer_details").path("email").asText(null));
        subscription.setStripeCheckoutSessionId(object.path("id").asText(null));
        subscription.setStripeSubscriptionId(object.path("subscription").asText(null));
        subscription.setCancelAtPeriodEnd(false);
        subscription.setCancellationReason(null);
        subscription.setStartsAt(LocalDate.now());
        subscription.setRenewsAt(LocalDate.now().plusDays(FREE_TRIAL_DAYS));
        subscription.setUpdatedAt(Instant.now());
        tenantSubscriptionRepository.save(subscription);
    }

    private void syncUpdatedSubscription(JsonNode object) {
        String stripeSubscriptionId = object.path("id").asText();
        tenantSubscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .ifPresent(subscription -> {
                    subscription.setStatus(mapStripeSubscriptionStatus(object.path("status").asText()));
                    subscription.setCancelAtPeriodEnd(object.path("cancel_at_period_end").asBoolean(false));
                    subscription.setRenewsAt(readStripeDate(object, "current_period_end", subscription.getRenewsAt()));
                    subscription.setCancelledAt(readStripeDate(object, "canceled_at", subscription.getCancelledAt()));
                    subscription.setUpdatedAt(Instant.now());
                    tenantSubscriptionRepository.save(subscription);
                });
    }

    private void markSubscriptionCancelled(JsonNode object) {
        String stripeSubscriptionId = object.path("id").asText();
        tenantSubscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .ifPresent(subscription -> {
                    subscription.setStatus(SubscriptionStatus.CANCELLED);
                    subscription.setCancelledAt(readStripeDate(object, "canceled_at", LocalDate.now()));
                    subscription.setCancelAtPeriodEnd(false);
                    subscription.setUpdatedAt(Instant.now());
                    tenantSubscriptionRepository.save(subscription);
                });
    }

    private void markSubscriptionPastDue(JsonNode object) {
        String stripeSubscriptionId = object.path("subscription").asText();
        tenantSubscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .ifPresent(subscription -> {
                    subscription.setStatus(SubscriptionStatus.PAST_DUE);
                    subscription.setUpdatedAt(Instant.now());
                    tenantSubscriptionRepository.save(subscription);
                });
    }

    public void markSubscriptionActiveFromInvoice(JsonNode object) {
        String stripeSubscriptionId = object.path("subscription").asText();
        tenantSubscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .ifPresent(subscription -> {
                    subscription.setStatus(SubscriptionStatus.ACTIVE);
                    subscription.setCancelAtPeriodEnd(false);
                    subscription.setCancelledAt(null);
                    if (subscription.getPendingPlanCode() != null) {
                        subscription.setPlanCode(subscription.getPendingPlanCode());
                        subscription.setPendingPlanCode(null);
                    }
                    subscription.setUpdatedAt(Instant.now());
                    tenantSubscriptionRepository.save(subscription);
                });
    }

    private int addOptionalRecurringLineItems(
            MultiValueMap<String, String> form,
            int formIndex,
            boolean smsOptionEnabled,
            boolean advancedAuditOptionEnabled,
            boolean advancedExportOptionEnabled) {
        if (smsOptionEnabled) {
            addRecurringLineItem(form, formIndex++, "SMS notifications option", SMS_OPTION_PRICE, smsOptionStripePriceId);
        }
        if (advancedAuditOptionEnabled) {
            addRecurringLineItem(form, formIndex++, "Advanced RH audit option", AUDIT_OPTION_PRICE, advancedAuditOptionStripePriceId);
        }
        if (advancedExportOptionEnabled) {
            addRecurringLineItem(form, formIndex++, "Advanced export option", EXPORT_OPTION_PRICE, advancedExportOptionStripePriceId);
        }
        return formIndex;
    }

    private JsonNode fetchStripeSubscription(String stripeSubscriptionId) {
        String response = restTemplate.exchange(
                stripeBaseUrl + "/v1/subscriptions/" + stripeSubscriptionId,
                HttpMethod.GET,
                new HttpEntity<>(stripeHeaders()),
                String.class
        ).getBody();
        try {
            return objectMapper.readTree(response);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to parse Stripe subscription response", exception);
        }
    }

    private HttpHeaders stripeHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(stripeSecretKey, "");
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    private boolean stripeConfigured() {
        return stripeSecretKey != null && !stripeSecretKey.isBlank();
    }

    private void addPaymentMethods(MultiValueMap<String, String> form, List<String> paymentMethodTypes) {
        for (int index = 0; index < paymentMethodTypes.size(); index++) {
            String paymentMethodType = paymentMethodTypes.get(index);
            if ("customer_balance".equals(paymentMethodType)) {
                throw new BadRequestException("Bank transfer via customer_balance is not supported in Stripe Checkout subscription mode");
            }
            form.add("payment_method_types[%d]".formatted(index), paymentMethodType);
        }
    }

    private void addRecurringLineItem(
            MultiValueMap<String, String> form,
            int index,
            String productName,
            BigDecimal amount,
            String stripePriceId) {
        if (stripePriceId != null && !stripePriceId.isBlank()) {
            form.add("line_items[%d][price]".formatted(index), stripePriceId);
        } else {
            form.add("line_items[%d][price_data][currency]".formatted(index), "eur");
            form.add("line_items[%d][price_data][unit_amount]".formatted(index), toStripeAmount(amount));
            form.add("line_items[%d][price_data][recurring][interval]".formatted(index), "month");
            form.add("line_items[%d][price_data][product_data][name]".formatted(index), productName);
        }
        form.add("line_items[%d][quantity]".formatted(index), "1");
    }

    private void addSubscriptionItemPrice(
            MultiValueMap<String, String> form,
            int index,
            String productName,
            BigDecimal amount,
            String stripePriceId) {
        if (stripePriceId != null && !stripePriceId.isBlank()) {
            form.add("items[%d][price]".formatted(index), stripePriceId);
            return;
        }
        form.add("items[%d][price_data][currency]".formatted(index), "eur");
        form.add("items[%d][price_data][unit_amount]".formatted(index), toStripeAmount(amount));
        form.add("items[%d][price_data][recurring][interval]".formatted(index), "month");
        form.add("items[%d][price_data][product_data][name]".formatted(index), productName);
    }

    private String toStripeAmount(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString();
    }

    private String extractStripeErrorMessage(HttpStatusCodeException exception) {
        String responseBody = exception.getResponseBodyAsString();
        if (responseBody != null && !responseBody.isBlank()) {
            try {
                JsonNode json = objectMapper.readTree(responseBody);
                String message = json.path("error").path("message").asText();
                if (message != null && !message.isBlank()) {
                    return "Stripe checkout error: " + message;
                }
            } catch (Exception ignored) {
                // Fall back to a generic message when Stripe does not return a JSON error payload.
            }
        }
        return "Stripe checkout error: " + exception.getStatusCode();
    }

    private SubscriptionPlan getPlan(PlanCode planCode) {
        return subscriptionPlanRepository.findByCode(planCode)
                .orElseThrow(() -> new NotFoundException("Subscription plan not found"));
    }

    private SubscriptionStatus mapStripeSubscriptionStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "trialing" -> SubscriptionStatus.TRIAL;
            case "active" -> SubscriptionStatus.ACTIVE;
            case "past_due", "unpaid", "incomplete", "incomplete_expired" -> SubscriptionStatus.PAST_DUE;
            case "canceled" -> SubscriptionStatus.CANCELLED;
            default -> SubscriptionStatus.ACTIVE;
        };
    }

    private LocalDate readStripeDate(JsonNode object, String fieldName, LocalDate fallback) {
        long epochSeconds = object.path(fieldName).asLong(0);
        if (epochSeconds <= 0) {
            return fallback;
        }
        return Instant.ofEpochSecond(epochSeconds).atZone(ZoneOffset.UTC).toLocalDate();
    }
}
