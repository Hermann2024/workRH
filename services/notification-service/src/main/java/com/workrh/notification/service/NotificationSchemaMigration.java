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
        widenSupportTicketTextColumns();
    }

    /**
     * Tickets store automatic context (URL, user agent); the default varchar(255) overflows and causes 500 errors.
     */
    private void widenSupportTicketTextColumns() {
        try {
            jdbcTemplate.execute("ALTER TABLE support_tickets ALTER COLUMN message TYPE TEXT");
        } catch (Exception ignored) {
            // Column may already be TEXT or table missing on first bootstrap order.
        }
        try {
            jdbcTemplate.execute("ALTER TABLE support_tickets ALTER COLUMN resolution_message TYPE TEXT");
        } catch (Exception ignored) {
            // Same as above.
        }
        try {
            jdbcTemplate.execute("ALTER TABLE support_tickets ALTER COLUMN subject TYPE VARCHAR(1024)");
        } catch (Exception ignored) {
            // Same as above.
        }
    }
}
