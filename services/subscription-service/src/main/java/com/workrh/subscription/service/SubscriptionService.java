package com.workrh.subscription.service;

import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.common.web.NotFoundException;
import com.workrh.subscription.api.dto.FeatureCheckResponse;
import com.workrh.subscription.api.dto.PlanResponse;
import com.workrh.subscription.api.dto.SubscriptionCancelRequest;
import com.workrh.subscription.api.dto.SubscriptionChangeRequest;
import com.workrh.subscription.api.dto.SubscriptionRequest;
import com.workrh.subscription.api.dto.SubscriptionResponse;
import com.workrh.subscription.domain.FeatureCode;
import com.workrh.subscription.domain.PlanCode;
import com.workrh.subscription.domain.SubscriptionPlan;
import com.workrh.subscription.domain.SubscriptionStatus;
import com.workrh.subscription.domain.TenantSubscription;
import com.workrh.subscription.repository.SubscriptionPlanRepository;
import com.workrh.subscription.repository.TenantSubscriptionRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final StripeCheckoutService stripeCheckoutService;

    public SubscriptionService(
            SubscriptionPlanRepository subscriptionPlanRepository,
            TenantSubscriptionRepository tenantSubscriptionRepository,
            StripeCheckoutService stripeCheckoutService) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.stripeCheckoutService = stripeCheckoutService;
    }

    public List<PlanResponse> listPlans() {
        return subscriptionPlanRepository.findAllByActiveTrue().stream().map(this::toPlanResponse).toList();
    }

    public SubscriptionResponse currentSubscription() {
        return toSubscriptionResponse(getTenantSubscription(), getPlan(getTenantSubscription().getPlanCode()));
    }

    public SubscriptionResponse upsertSubscription(SubscriptionRequest request) {
        SubscriptionPlan plan = getPlan(request.planCode());
        validateSeats(plan, request.seatsPurchased());

        TenantSubscription subscription = tenantSubscriptionRepository.findByTenantId(TenantContext.getTenantId())
                .orElseGet(TenantSubscription::new);
        subscription.setTenantId(TenantContext.getTenantId());
        subscription.setPlanId(plan.getId());
        subscription.setPlanCode(plan.getCode());
        subscription.setStatus(request.status());
        subscription.setSeatsPurchased(request.seatsPurchased());
        subscription.setSmsOptionEnabled(request.smsOptionEnabled());
        subscription.setAdvancedAuditOptionEnabled(request.advancedAuditOptionEnabled());
        subscription.setAdvancedExportOptionEnabled(request.advancedExportOptionEnabled());
        subscription.setStartsAt(request.startsAt());
        subscription.setRenewsAt(request.renewsAt());
        subscription.setUpdatedAt(Instant.now());

        TenantSubscription saved = tenantSubscriptionRepository.save(subscription);
        return toSubscriptionResponse(saved, plan);
    }

    public FeatureCheckResponse checkFeature(String featureName) {
        FeatureCode featureCode = parseFeature(featureName);
        TenantSubscription subscription = getTenantSubscription();
        if (subscription.getStatus() == SubscriptionStatus.TRIAL
                && subscription.getRenewsAt() != null
                && subscription.getRenewsAt().isBefore(LocalDate.now())) {
            return new FeatureCheckResponse(
                    subscription.getTenantId(),
                    featureCode.name(),
                    false,
                    "Free trial expired, purchase a subscription to continue",
                    subscription.getPlanCode().name()
            );
        }
        SubscriptionPlan plan = getPlan(subscription.getPlanCode());

        boolean allowed = plan.getFeatures().contains(featureCode) || isUpsellAllowed(featureCode, subscription);
        String reason = allowed ? "Feature enabled for current subscription" : "Upgrade required";
        return new FeatureCheckResponse(subscription.getTenantId(), featureCode.name(), allowed, reason, subscription.getPlanCode().name());
    }

    public SubscriptionResponse upgrade(SubscriptionChangeRequest request) {
        return changePlan(request, true);
    }

    public SubscriptionResponse downgrade(SubscriptionChangeRequest request) {
        return changePlan(request, false);
    }

    public SubscriptionResponse cancel(SubscriptionCancelRequest request) {
        TenantSubscription subscription = getTenantSubscription();
        stripeCheckoutService.scheduleCancelAtPeriodEnd(subscription);
        subscription.setCancelAtPeriodEnd(true);
        subscription.setCancellationReason(request.reason());
        subscription.setUpdatedAt(Instant.now());
        TenantSubscription saved = tenantSubscriptionRepository.save(subscription);
        return toSubscriptionResponse(saved, getPlan(saved.getPlanCode()));
    }

    public SubscriptionResponse reactivate() {
        TenantSubscription subscription = getTenantSubscription();
        stripeCheckoutService.reactivateCancellation(subscription);
        subscription.setCancelAtPeriodEnd(false);
        subscription.setCancellationReason(null);
        subscription.setUpdatedAt(Instant.now());
        TenantSubscription saved = tenantSubscriptionRepository.save(subscription);
        return toSubscriptionResponse(saved, getPlan(saved.getPlanCode()));
    }

    private SubscriptionResponse changePlan(SubscriptionChangeRequest request, boolean upgrade) {
        TenantSubscription subscription = getTenantSubscription();
        SubscriptionPlan currentPlan = getPlan(subscription.getPlanCode());
        SubscriptionPlan targetPlan = getPlan(request.targetPlanCode());
        validateSeats(targetPlan, request.seatsPurchased());

        if (upgrade
                && !targetPlan.isCustomPricing()
                && currentPlan.getMonthlyPrice().compareTo(targetPlan.getMonthlyPrice()) > 0) {
            throw new BadRequestException("Target plan is not an upgrade");
        }
        if (!upgrade
                && !currentPlan.isCustomPricing()
                && currentPlan.getMonthlyPrice().compareTo(targetPlan.getMonthlyPrice()) < 0) {
            throw new BadRequestException("Target plan is not a downgrade");
        }

        subscription.setPlanId(targetPlan.getId());
        subscription.setPlanCode(targetPlan.getCode());
        subscription.setSeatsPurchased(request.seatsPurchased());
        subscription.setSmsOptionEnabled(request.smsOptionEnabled());
        subscription.setAdvancedAuditOptionEnabled(request.advancedAuditOptionEnabled());
        subscription.setAdvancedExportOptionEnabled(request.advancedExportOptionEnabled());
        subscription.setPendingPlanCode(targetPlan.getCode());
        stripeCheckoutService.syncExistingSubscriptionChange(
                subscription,
                targetPlan,
                request.smsOptionEnabled(),
                request.advancedAuditOptionEnabled(),
                request.advancedExportOptionEnabled()
        );
        subscription.setPlanCode(targetPlan.getCode());
        subscription.setPendingPlanCode(null);
        subscription.setUpdatedAt(Instant.now());

        TenantSubscription saved = tenantSubscriptionRepository.save(subscription);
        return toSubscriptionResponse(saved, targetPlan);
    }

    private FeatureCode parseFeature(String featureName) {
        try {
            return FeatureCode.valueOf(featureName);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Unknown feature code");
        }
    }

    private boolean isUpsellAllowed(FeatureCode featureCode, TenantSubscription subscription) {
        return switch (featureCode) {
            case SMS_NOTIFICATIONS -> subscription.isSmsOptionEnabled();
            case DECLARATION_AUDIT -> subscription.isAdvancedAuditOptionEnabled();
            case EXPORTS, ACCOUNTING_EXPORT -> subscription.isAdvancedExportOptionEnabled();
            default -> false;
        };
    }

    private void validateSeats(SubscriptionPlan plan, int seatsPurchased) {
        if (plan.getMinEmployees() != null && seatsPurchased < plan.getMinEmployees()) {
            throw new BadRequestException("Seats below minimum plan size");
        }
        if (plan.getMaxEmployees() != null && seatsPurchased > plan.getMaxEmployees()) {
            throw new BadRequestException("Seats above maximum plan size");
        }
    }

    private TenantSubscription getTenantSubscription() {
        return tenantSubscriptionRepository.findByTenantId(TenantContext.getTenantId())
                .orElseThrow(() -> new NotFoundException("Tenant subscription not found"));
    }

    private SubscriptionPlan getPlan(PlanCode code) {
        return subscriptionPlanRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Subscription plan not found"));
    }

    private PlanResponse toPlanResponse(SubscriptionPlan plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getMonthlyPrice(),
                plan.getStripePriceId(),
                plan.getBillingCycle(),
                plan.getMinEmployees(),
                plan.getMaxEmployees(),
                plan.isRecommended(),
                plan.isCustomPricing(),
                plan.isActive(),
                plan.getFeatures()
        );
    }

    private SubscriptionResponse toSubscriptionResponse(TenantSubscription subscription, SubscriptionPlan plan) {
        Set<String> entitlements = plan.getFeatures().stream().map(Enum::name).collect(Collectors.toSet());
        if (subscription.isSmsOptionEnabled()) {
            entitlements.add(FeatureCode.SMS_NOTIFICATIONS.name());
        }
        if (subscription.isAdvancedAuditOptionEnabled()) {
            entitlements.add(FeatureCode.DECLARATION_AUDIT.name());
        }
        if (subscription.isAdvancedExportOptionEnabled()) {
            entitlements.add(FeatureCode.ACCOUNTING_EXPORT.name());
            entitlements.add(FeatureCode.EXPORTS.name());
        }
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getTenantId(),
                subscription.getPlanCode(),
                subscription.getPendingPlanCode(),
                subscription.getStatus(),
                subscription.getSeatsPurchased(),
                subscription.isSmsOptionEnabled(),
                subscription.isAdvancedAuditOptionEnabled(),
                subscription.isAdvancedExportOptionEnabled(),
                subscription.isCancelAtPeriodEnd(),
                subscription.getCancellationReason(),
                subscription.getStartsAt(),
                subscription.getRenewsAt(),
                subscription.getCancelledAt(),
                subscription.getStripeCustomerEmail(),
                subscription.getStripeCheckoutSessionId(),
                subscription.getStripeSubscriptionId(),
                entitlements
        );
    }
}
