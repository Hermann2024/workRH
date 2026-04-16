package com.workrh.subscription.repository;

import com.workrh.subscription.domain.SubscriptionInvoice;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionInvoiceRepository extends JpaRepository<SubscriptionInvoice, Long> {
    boolean existsByStripeInvoiceId(String stripeInvoiceId);

    List<SubscriptionInvoice> findAllByTenantIdOrderByIssuedAtDesc(String tenantId);
}
