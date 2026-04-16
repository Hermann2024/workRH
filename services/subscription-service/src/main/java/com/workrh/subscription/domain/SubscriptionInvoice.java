package com.workrh.subscription.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "subscription_invoices", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subscription_invoice_stripe", columnNames = "stripeInvoiceId")
})
public class SubscriptionInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tenantId;
    private Long subscriptionId;
    private String stripeInvoiceId;
    private String invoiceNumber;
    private String applicationName;
    private String customerEmail;
    private String customerName;

    @Enumerated(EnumType.STRING)
    private PlanCode planCode;

    @Column(scale = 2, precision = 10)
    private BigDecimal totalAmount;

    private String currency;
    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;
    private Instant issuedAt;
    private Instant createdAt = Instant.now();
}
