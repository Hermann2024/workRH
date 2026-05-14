package com.workrh.common.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class ProductionReadinessGuardTest {

    @Test
    void doesNothingWhenProductionGuardIsDisabled() {
        ProductionReadinessGuard guard = new ProductionReadinessGuard(new MockEnvironment());

        assertThatCode(() -> guard.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsUnsafeProductionConfiguration() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("workrh.production-readiness.enforced", "true")
                .withProperty("spring.application.name", "subscription-service")
                .withProperty("security.demo-authentication-enabled", "true")
                .withProperty("security.jwt.secret", "too-short")
                .withProperty("spring.jpa.hibernate.ddl-auto", "update")
                .withProperty("subscription.bootstrap.key", "workrh-subscription-bootstrap");
        ProductionReadinessGuard guard = new ProductionReadinessGuard(environment);

        assertThatThrownBy(() -> guard.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("demo-authentication-enabled")
                .hasMessageContaining("ddl-auto=update")
                .hasMessageContaining("stripe.secret-key");
    }

    @Test
    void acceptsMinimalProductionSubscriptionConfiguration() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("workrh.production-readiness.enforced", "true")
                .withProperty("spring.application.name", "subscription-service")
                .withProperty("security.demo-authentication-enabled", "false")
                .withProperty("security.jwt.secret", "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=")
                .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
                .withProperty("subscription.bootstrap.key", "subscription-bootstrap-secret")
                .withProperty("workspace.internal.key", "workspace-secret")
                .withProperty("stripe.secret-key", "sk_live_secret")
                .withProperty("stripe.webhook.secret", "whsec_secret")
                .withProperty("stripe.prices.starter", "price_starter")
                .withProperty("stripe.prices.pro", "price_pro")
                .withProperty("stripe.prices.premium", "price_premium");
        ProductionReadinessGuard guard = new ProductionReadinessGuard(environment);

        assertThatCode(() -> guard.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();
    }
}
