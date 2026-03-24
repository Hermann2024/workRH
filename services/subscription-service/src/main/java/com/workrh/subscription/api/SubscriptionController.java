package com.workrh.subscription.api;

import com.workrh.subscription.api.dto.FeatureCheckResponse;
import com.workrh.subscription.api.dto.PlanResponse;
import com.workrh.subscription.api.dto.StripeCheckoutRequest;
import com.workrh.subscription.api.dto.StripeCheckoutResponse;
import com.workrh.subscription.api.dto.SubscriptionCancelRequest;
import com.workrh.subscription.api.dto.SubscriptionChangeRequest;
import com.workrh.subscription.api.dto.SubscriptionRequest;
import com.workrh.subscription.api.dto.SubscriptionResponse;
import com.workrh.subscription.service.StripeCheckoutService;
import com.workrh.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import java.util.List;
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

    public SubscriptionController(SubscriptionService subscriptionService, StripeCheckoutService stripeCheckoutService) {
        this.subscriptionService = subscriptionService;
        this.stripeCheckoutService = stripeCheckoutService;
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

    @PostMapping("/current")
    @PreAuthorize("hasAuthority('ADMIN')")
    public SubscriptionResponse upsert(@Valid @RequestBody SubscriptionRequest request) {
        return subscriptionService.upsertSubscription(request);
    }

    @GetMapping("/features/check")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR','EMPLOYEE')")
    public FeatureCheckResponse check(@RequestParam String feature) {
        return subscriptionService.checkFeature(feature);
    }

    @PatchMapping("/current/upgrade")
    @PreAuthorize("hasAuthority('ADMIN')")
    public SubscriptionResponse upgrade(@Valid @RequestBody SubscriptionChangeRequest request) {
        return subscriptionService.upgrade(request);
    }

    @PatchMapping("/current/downgrade")
    @PreAuthorize("hasAuthority('ADMIN')")
    public SubscriptionResponse downgrade(@Valid @RequestBody SubscriptionChangeRequest request) {
        return subscriptionService.downgrade(request);
    }

    @PatchMapping("/current/cancel")
    @PreAuthorize("hasAuthority('ADMIN')")
    public SubscriptionResponse cancel(@Valid @RequestBody SubscriptionCancelRequest request) {
        return subscriptionService.cancel(request);
    }

    @PatchMapping("/current/reactivate")
    @PreAuthorize("hasAuthority('ADMIN')")
    public SubscriptionResponse reactivate() {
        return subscriptionService.reactivate();
    }

    @PostMapping("/checkout/stripe")
    @PreAuthorize("hasAuthority('ADMIN')")
    public StripeCheckoutResponse createStripeCheckout(@Valid @RequestBody StripeCheckoutRequest request) {
        return stripeCheckoutService.createCheckoutSession(request);
    }

    @PostMapping("/webhooks/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestHeader("Stripe-Signature") String stripeSignature,
            @RequestBody String payload) {
        stripeCheckoutService.handleWebhook(payload, stripeSignature);
        return ResponseEntity.ok().build();
    }
}
