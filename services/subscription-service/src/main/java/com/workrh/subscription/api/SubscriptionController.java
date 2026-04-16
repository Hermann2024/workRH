package com.workrh.subscription.api;

import com.workrh.subscription.api.dto.FeatureCheckResponse;
import com.workrh.subscription.api.dto.PlanResponse;
import com.workrh.subscription.api.dto.SubscriptionBootstrapRequest;
import com.workrh.subscription.api.dto.StripeCheckoutRequest;
import com.workrh.subscription.api.dto.StripeCheckoutResponse;
import com.workrh.subscription.api.dto.SubscriptionCancelRequest;
import com.workrh.subscription.api.dto.SubscriptionChangeRequest;
import com.workrh.subscription.api.dto.SubscriptionRequest;
import com.workrh.subscription.api.dto.SubscriptionResponse;
import com.workrh.subscription.domain.FeatureCode;
import com.workrh.subscription.service.StripeCheckoutService;
import com.workrh.subscription.service.SubscriptionInvoiceExportService;
import com.workrh.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final StripeCheckoutService stripeCheckoutService;
    private final SubscriptionInvoiceExportService subscriptionInvoiceExportService;

    public SubscriptionController(
            SubscriptionService subscriptionService,
            StripeCheckoutService stripeCheckoutService,
            SubscriptionInvoiceExportService subscriptionInvoiceExportService) {
        this.subscriptionService = subscriptionService;
        this.stripeCheckoutService = stripeCheckoutService;
        this.subscriptionInvoiceExportService = subscriptionInvoiceExportService;
    }

    @GetMapping("/plans")
    public List<PlanResponse> plans() {
        return subscriptionService.listPlans();
    }

    @GetMapping("/current")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public SubscriptionResponse current() {
        return subscriptionService.currentSubscription();
    }

    @PostMapping("/bootstrap")
    public SubscriptionResponse bootstrap(
            @RequestHeader("X-Bootstrap-Key") String bootstrapKey,
            @Valid @RequestBody SubscriptionBootstrapRequest request) {
        return subscriptionService.bootstrapSelfServiceSubscription(request, bootstrapKey);
    }

    @PostMapping("/current")
    @PreAuthorize("hasAuthority('ADMIN')")
    public SubscriptionResponse upsert(@Valid @RequestBody SubscriptionRequest request) {
        return subscriptionService.upsertSubscription(request);
    }

    @GetMapping("/features/check")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR','EMPLOYEE')")
    public FeatureCheckResponse check(@RequestParam("feature") String feature) {
        return subscriptionService.checkFeature(feature);
    }

    @PatchMapping("/current/upgrade")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public SubscriptionResponse upgrade(@Valid @RequestBody SubscriptionChangeRequest request) {
        return subscriptionService.upgrade(request);
    }

    @PatchMapping("/current/downgrade")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public SubscriptionResponse downgrade(@Valid @RequestBody SubscriptionChangeRequest request) {
        return subscriptionService.downgrade(request);
    }

    @PatchMapping("/current/cancel")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public SubscriptionResponse cancel(@Valid @RequestBody SubscriptionCancelRequest request) {
        return subscriptionService.cancel(request);
    }

    @PatchMapping("/current/reactivate")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public SubscriptionResponse reactivate() {
        return subscriptionService.reactivate();
    }

    @PostMapping("/checkout/stripe")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public StripeCheckoutResponse createStripeCheckout(@Valid @RequestBody StripeCheckoutRequest request) {
        return stripeCheckoutService.createCheckoutSession(request);
    }

    @PostMapping("/checkout/stripe/confirm")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public SubscriptionResponse confirmStripeCheckout(@RequestParam("sessionId") String sessionId) {
        stripeCheckoutService.confirmCheckoutSession(sessionId);
        return subscriptionService.currentSubscription();
    }

    @PostMapping("/webhooks/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestHeader("Stripe-Signature") String stripeSignature,
            @RequestBody String payload) {
        stripeCheckoutService.handleWebhook(payload, stripeSignature);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/invoices/accounting-export/csv", produces = "text/csv")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public ResponseEntity<byte[]> exportAccountingCsv(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "month", required = false) Integer month) {
        subscriptionService.ensureFeatureEnabled(FeatureCode.ACCOUNTING_EXPORT);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("workrh-accounting-export.csv").build().toString()
                )
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(subscriptionInvoiceExportService.exportAccountingCsv(year, month));
    }
}
