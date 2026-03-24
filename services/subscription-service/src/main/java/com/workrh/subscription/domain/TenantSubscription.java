package com.workrh.subscription.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tenant_subscriptions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subscription_tenant", columnNames = "tenantId")
})
public class TenantSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tenantId;
    private Long planId;
    @Enumerated(EnumType.STRING)
    private PlanCode planCode;
    @Enumerated(EnumType.STRING)
    private PlanCode pendingPlanCode;
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;
    private int seatsPurchased;
    private String stripeCustomerEmail;
    private String stripeCheckoutSessionId;
    private String stripeSubscriptionId;
    private boolean smsOptionEnabled;
    private boolean advancedAuditOptionEnabled;
    private boolean advancedExportOptionEnabled;
    private boolean cancelAtPeriodEnd;
    private String cancellationReason;
    private LocalDate startsAt;
    private LocalDate renewsAt;
    private LocalDate cancelledAt;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
