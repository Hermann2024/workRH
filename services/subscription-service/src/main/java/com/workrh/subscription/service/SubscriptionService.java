package com.workrh.subscription.service;

import com.workrh.common.security.SecurityUtils;
import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.common.web.NotFoundException;
import com.workrh.common.web.UnauthorizedException;
import com.workrh.subscription.api.dto.CatalogReadinessResponse;
import com.workrh.subscription.api.dto.FeatureCheckResponse;
import com.workrh.subscription.api.dto.PlanResponse;
import com.workrh.subscription.api.dto.ServiceModuleResponse;
import com.workrh.subscription.api.dto.SubscriptionBootstrapRequest;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class SubscriptionService {

    private static final int SELF_SERVICE_TRIAL_DAYS = 14;

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final TenantSubscriptionRepository tenantSubscriptionRepository;
    private final StripeCheckoutService stripeCheckoutService;
    private final WorkspaceSubscriptionSyncClient workspaceSubscriptionSyncClient;

    @Value("${subscription.preview-all-features.enabled:false}")
    private boolean previewAllFeaturesEnabled;

    @Value("${subscription.preview-all-features.user-emails:}")
    private String previewAllFeaturesUserEmails;

    @Value("${subscription.bootstrap.key:workrh-signup-bootstrap}")
    private String subscriptionBootstrapKey;

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${stripe.webhook.secret:}")
    private String stripeWebhookSecret;

    @Value("${notification.support.smtp-host:}")
    private String supportSmtpHost;

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${notification.sms.webhook-url:}")
    private String smsWebhookUrl;

    @Value("${subscription.enterprise.enabled:false}")
    private boolean enterprisePlanEnabled;

    public SubscriptionService(
            SubscriptionPlanRepository subscriptionPlanRepository,
            TenantSubscriptionRepository tenantSubscriptionRepository,
            StripeCheckoutService stripeCheckoutService,
            WorkspaceSubscriptionSyncClient workspaceSubscriptionSyncClient) {
        this.subscriptionPlanRepository = subscriptionPlanRepository;
        this.tenantSubscriptionRepository = tenantSubscriptionRepository;
        this.stripeCheckoutService = stripeCheckoutService;
        this.workspaceSubscriptionSyncClient = workspaceSubscriptionSyncClient;
    }

    public List<PlanResponse> listPlans() {
        return subscriptionPlanRepository.findAllByActiveTrue().stream().map(this::toPlanResponse).toList();
    }

    public CatalogReadinessResponse catalogReadiness() {
        List<String> warnings = new ArrayList<>();
        if (!isStripeCheckoutAvailable()) {
            warnings.add("Le paiement Stripe n'est pas configuré pour cet environnement.");
        }
        return new CatalogReadinessResponse(
                isStripeCheckoutAvailable(),
                isSupportAcknowledgementEmailAvailable(),
                isSmsDeliveryAvailable(),
                enterprisePlanEnabled,
                warnings
        );
    }

    public SubscriptionResponse currentSubscription() {
        return toSubscriptionResponse(getTenantSubscription(), getPlan(getTenantSubscription().getPlanCode()));
    }

    public List<ServiceModuleResponse> serviceModules() {
        TenantSubscription subscription = getTenantSubscription();
        SubscriptionPlan plan = getPlan(subscription.getPlanCode());
        Set<String> entitlements = hasPreviewAllFeaturesAccess()
                ? Arrays.stream(FeatureCode.values()).map(Enum::name).collect(Collectors.toSet())
                : buildEntitlements(subscription, plan);

        return List.of(
                module(FeatureCode.DASHBOARD_BASIC, "Tableau de bord RH de base", "Pilotage RH",
                        "Vue consolidée des salariés suivis, jours utilisés, jours restants et alertes principales.",
                        "reporting-service: /api/reports/dashboard", "Ouvrir le dashboard", "/dashboard", entitlements),
                module(FeatureCode.DASHBOARD_ADVANCED, "Tableau de bord avance", "Pilotage RH",
                        "Synthèse entreprise avec détail annuel, hebdomadaire, fiscal et politique par salarié.",
                        "telework-service: /api/telework/company-summary", "Voir la vue avancee", "/dashboard", entitlements),
                module(FeatureCode.MONTHLY_STATS, "Statistiques mensuelles", "Reporting",
                        "Lecture annuelle mois par mois des jours de télétravail, des salariés suivis et des alertes.",
                        "reporting-service: /api/reports/monthly-stats", "Consulter les statistiques", "/dashboard", entitlements),
                module(FeatureCode.EXPORTS, "Exports de données", "Reporting",
                        "Exports CSV, PDF et synthèse texte des données RH et télétravail du mois.",
                        "reporting-service: /api/reports/dashboard/export/{format}", "Exporter les données", "/dashboard", entitlements),
                module(FeatureCode.TELEWORK_BASIC, "Suivi simple du teletravail", "Teletravail",
                        "Déclaration salarié et lecture RH des derniers jours de télétravail du même tenant.",
                        "telework-service: /api/telework, /api/telework/recent", "Voir les declarations", "/dashboard", entitlements),
                module(FeatureCode.TELEWORK_COMPLIANCE_34, "Conformite teletravail frontalier Luxembourg 34 jours", "Teletravail",
                        "Calcul du seuil fiscal frontalier, jours restants, dépassements et règles applicables par pays.",
                        "telework-service: /api/telework/summary/{employeeId}, /api/telework/policies", "Gérer les règles", "/policies", entitlements),
                module(FeatureCode.AUTO_EXCLUSION, "Exclusions automatiques des jours non comptables", "Teletravail",
                        "Les congés et arrêts maladie alimentent les périodes exclues afin d'éviter de compter des jours non travaillables.",
                        "telework-service: exclusions internes + evenements leave/sickness", "Controler les exclusions", "/dashboard", entitlements),
                module(FeatureCode.THRESHOLD_ALERTS, "Alertes de depassement de seuil", "Alertes",
                        "Détection des dépassements fiscaux ou hebdomadaires, avec journalisation pour la RH.",
                        "telework-service + notification-service", "Voir les alertes", "/dashboard", entitlements),
                module(FeatureCode.EMPLOYEE_MANAGEMENT, "Gestion des employés", "Administration",
                        "Création, modification, activation, désactivation et suppression des comptes salariés.",
                        "user-service: /api/users", "Gérer les salariés", "/employees", entitlements),
                module(FeatureCode.LEAVE_MANAGEMENT, "Gestion des congés", "Absences",
                        "Dépôt salarié, validation RH, refus RH et exclusion automatique des périodes validées.",
                        "leave-service: /api/leaves", "Traiter les congés", "/dashboard", entitlements),
                module(FeatureCode.SICKNESS_MANAGEMENT, "Gestion des arrêts maladie", "Absences",
                        "Déclaration salarié et vue RH des arrêts maladie de la même société.",
                        "sickness-service: /api/sickness", "Voir les arrêts maladie", "/dashboard", entitlements),
                module(FeatureCode.EMAIL_NOTIFICATIONS, "Notifications email automatiques", "Alertes",
                        "Journal des notifications automatiques émises ou journalisées pour les alertes RH.",
                        "notification-service: /api/notifications", "Voir le journal", "/dashboard", entitlements),
                module(FeatureCode.EMAIL_SUPPORT, "Support email", "Support",
                        "Création de tickets support standard rattachés au tenant.",
                        "notification-service: /api/support/tickets", "Contacter le support", "/billing", entitlements),
                module(FeatureCode.PRIORITY_SUPPORT, "Support prioritaire", "Support",
                        "Tickets support priorisés avec suivi SLA quand le plan le permet.",
                        "notification-service: /api/support/tickets/priority", "Ouvrir un ticket prioritaire", "/billing", entitlements)
        );
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

    public SubscriptionResponse bootstrapSelfServiceSubscription(SubscriptionBootstrapRequest request, String bootstrapKey) {
        validateBootstrapKey(bootstrapKey);

        PlanCode planCode = request.planCode() == null ? PlanCode.STARTER : request.planCode();
        SubscriptionPlan plan = getPlan(planCode);
        if (plan.isCustomPricing()) {
            throw new BadRequestException("Enterprise plan cannot be initialized in self-service");
        }
        validateSeats(plan, request.seatsPurchased());

        TenantSubscription subscription = tenantSubscriptionRepository.findByTenantId(TenantContext.getTenantId())
                .orElseGet(TenantSubscription::new);
        if (subscription.getId() != null) {
            return toSubscriptionResponse(subscription, getPlan(subscription.getPlanCode()));
        }

        subscription.setTenantId(TenantContext.getTenantId());
        subscription.setPlanId(plan.getId());
        subscription.setPlanCode(planCode);
        subscription.setPendingPlanCode(null);
        subscription.setStatus(SubscriptionStatus.TRIAL);
        subscription.setSeatsPurchased(request.seatsPurchased());
        subscription.setStripeCustomerEmail(request.ownerEmail().trim().toLowerCase());
        subscription.setSmsOptionEnabled(false);
        subscription.setAdvancedAuditOptionEnabled(false);
        subscription.setAdvancedExportOptionEnabled(false);
        subscription.setCancelAtPeriodEnd(false);
        subscription.setCancellationReason(null);
        subscription.setStartsAt(LocalDate.now());
        subscription.setRenewsAt(LocalDate.now().plusDays(SELF_SERVICE_TRIAL_DAYS));
        subscription.setCancelledAt(null);
        subscription.setUpdatedAt(Instant.now());

        TenantSubscription saved = tenantSubscriptionRepository.save(subscription);
        return toSubscriptionResponse(saved, plan);
    }

    public FeatureCheckResponse checkFeature(String featureName) {
        FeatureCode featureCode = parseFeature(featureName);
        TenantSubscription subscription = getTenantSubscription();
        boolean previewOverride = hasPreviewAllFeaturesAccess();
        boolean allowed = previewOverride || hasFeature(subscription, featureCode);
        String reason = previewOverride
                ? "Preview override enabled for current user"
                : allowed ? "Feature enabled for current subscription" : "Upgrade required";
        return new FeatureCheckResponse(subscription.getTenantId(), featureCode.name(), allowed, reason, subscription.getPlanCode().name());
    }

    public void ensureFeatureEnabled(FeatureCode featureCode) {
        if (hasPreviewAllFeaturesAccess()) {
            return;
        }
        TenantSubscription subscription = getTenantSubscription();
        if (!hasFeature(subscription, featureCode)) {
            throw new AccessDeniedException("Upgrade required");
        }
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
        workspaceSubscriptionSyncClient.sync(saved);
        return toSubscriptionResponse(saved, getPlan(saved.getPlanCode()));
    }

    public SubscriptionResponse reactivate() {
        TenantSubscription subscription = getTenantSubscription();
        stripeCheckoutService.reactivateCancellation(subscription);
        subscription.setCancelAtPeriodEnd(false);
        subscription.setCancellationReason(null);
        subscription.setUpdatedAt(Instant.now());
        TenantSubscription saved = tenantSubscriptionRepository.save(subscription);
        workspaceSubscriptionSyncClient.sync(saved);
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
        workspaceSubscriptionSyncClient.sync(saved);
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

    private boolean hasFeature(TenantSubscription subscription, FeatureCode featureCode) {
        if (subscription.getStatus() == SubscriptionStatus.TRIAL
                && subscription.getRenewsAt() != null
                && subscription.getRenewsAt().isBefore(LocalDate.now())) {
            return false;
        }
        SubscriptionPlan plan = getPlan(subscription.getPlanCode());
        return plan.getFeatures().contains(featureCode) || isUpsellAllowed(featureCode, subscription);
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

    private ServiceModuleResponse module(
            FeatureCode featureCode,
            String name,
            String category,
            String description,
            String backendScope,
            String actionLabel,
            String actionRoute,
            Set<String> entitlements) {
        return new ServiceModuleResponse(
                featureCode.name(),
                name,
                category,
                description,
                backendScope,
                actionLabel,
                actionRoute,
                entitlements.contains(featureCode.name())
        );
    }

    private SubscriptionResponse toSubscriptionResponse(TenantSubscription subscription, SubscriptionPlan plan) {
        boolean previewAllFeaturesActive = hasPreviewAllFeaturesAccess();
        Set<String> entitlements = previewAllFeaturesActive
                ? Arrays.stream(FeatureCode.values()).map(Enum::name).collect(Collectors.toSet())
                : buildEntitlements(subscription, plan);
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
                previewAllFeaturesActive,
                entitlements
        );
    }

    private Set<String> buildEntitlements(TenantSubscription subscription, SubscriptionPlan plan) {
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
        return entitlements;
    }

    private boolean hasPreviewAllFeaturesAccess() {
        if (!previewAllFeaturesEnabled) {
            return false;
        }
        String username = SecurityUtils.currentUsername();
        if (username == null) {
            return false;
        }
        return Arrays.stream(previewAllFeaturesUserEmails.split("[,;]"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(String::toLowerCase)
                .anyMatch(username.trim().toLowerCase()::equals);
    }

    private void validateBootstrapKey(String bootstrapKey) {
        if (bootstrapKey == null || bootstrapKey.isBlank() || !bootstrapKey.equals(subscriptionBootstrapKey)) {
            throw new UnauthorizedException("Invalid bootstrap key");
        }
    }

    private boolean isStripeCheckoutAvailable() {
        return stripeSecretKey != null
                && !stripeSecretKey.isBlank()
                && stripeWebhookSecret != null
                && !stripeWebhookSecret.isBlank();
    }

    private boolean isSupportAcknowledgementEmailAvailable() {
        return supportSmtpHost != null && !supportSmtpHost.isBlank();
    }

    private boolean isSmsDeliveryAvailable() {
        return smsEnabled && smsWebhookUrl != null && !smsWebhookUrl.isBlank();
    }
}
