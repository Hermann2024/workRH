package com.workrh.subscription.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(5)
public class SubscriptionSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public SubscriptionSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_tenant_subscriptions_tenant ON tenant_subscriptions (tenant_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_subscription_invoices_tenant ON subscription_invoices (tenant_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_subscription_invoices_tenant_issued ON subscription_invoices (tenant_id, issued_at)");
    }
}
