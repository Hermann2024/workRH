package com.workrh.reporting.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(5)
public class ReportingSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public ReportingSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_metric_snapshots_tenant ON telework_metric_snapshots (tenant_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_metric_snapshots_tenant_employee ON telework_metric_snapshots (tenant_id, employee_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_metric_snapshots_tenant_period ON telework_metric_snapshots (tenant_id, year, month)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_metric_snapshots_tenant_employee_period ON telework_metric_snapshots (tenant_id, employee_id, year, month)");
    }
}
