package com.workrh.notification.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(5)
public class NotificationSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public NotificationSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_notification_logs_tenant ON notification_logs (tenant_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_notification_logs_tenant_employee ON notification_logs (tenant_id, employee_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_notification_logs_tenant_sent ON notification_logs (tenant_id, sent_at)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_support_tickets_tenant ON support_tickets (tenant_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_support_tickets_tenant_status ON support_tickets (tenant_id, status)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_support_tickets_tenant_created ON support_tickets (tenant_id, created_at)");
    }
}
