package com.workrh.subscription.api.dto;

public record StripeCheckoutResponse(String sessionId, String checkoutUrl) {
}
