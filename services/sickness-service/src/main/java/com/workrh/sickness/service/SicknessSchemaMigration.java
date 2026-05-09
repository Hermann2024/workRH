package com.workrh.sickness.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(5)
public class SicknessSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public SicknessSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_sickness_records_tenant ON sickness_records (tenant_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_sickness_records_tenant_employee ON sickness_records (tenant_id, employee_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_sickness_records_tenant_dates ON sickness_records (tenant_id, start_date, end_date)");
    }
}
