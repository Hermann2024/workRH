package com.workrh.subscription.repository;

import com.workrh.subscription.domain.TenantSubscription;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantSubscriptionRepository extends JpaRepository<TenantSubscription, Long> {
    Optional<TenantSubscription> findByTenantId(String tenantId);
    Optional<TenantSubscription> findByStripeSubscriptionId(String stripeSubscriptionId);
}
