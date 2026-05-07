package com.workrh.users.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class RoleSchemaMigrator implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public RoleSchemaMigrator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = 'public'
                          AND table_name = 'employee_roles'
                    ) THEN
                        ALTER TABLE employee_roles
                            DROP CONSTRAINT IF EXISTS employee_roles_role_check;

                        ALTER TABLE employee_roles
                            ADD CONSTRAINT employee_roles_role_check
                            CHECK (role IN ('PLATFORM_ADMIN', 'ADMIN', 'HR', 'EMPLOYEE'));
                    END IF;
                END $$;
                """);
    }
}
