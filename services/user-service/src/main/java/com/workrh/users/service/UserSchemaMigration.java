package com.workrh.users.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(5)
public class UserSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public UserSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_employees_tenant ON employees (tenant_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_employee_roles_employee ON employee_roles (employee_id)");
    }
}
