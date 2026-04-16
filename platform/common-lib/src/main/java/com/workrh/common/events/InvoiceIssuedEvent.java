package com.workrh.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record InvoiceIssuedEvent(
        String tenantId,
        String invoiceNumber,
        String stripeInvoiceId,
        String applicationName,
        String customerEmail,
        String customerName,
        String planCode,
        BigDecimal totalAmount,
        String currency,
        LocalDate billingPeriodStart,
        LocalDate billingPeriodEnd,
        List<InvoiceLineItemEvent> lineItems,
        Instant occurredAt
) implements DomainEvent {
    @Override
    public String type() {
        return "billing.invoice.issued";
    }
}
