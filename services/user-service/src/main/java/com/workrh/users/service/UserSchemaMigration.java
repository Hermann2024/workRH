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
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS tenant_workspaces (
                    tenant_id VARCHAR(120) PRIMARY KEY,
                    company_name VARCHAR(255) NOT NULL,
                    owner_email VARCHAR(255),
                    plan_code VARCHAR(40),
                    seats_purchased INTEGER,
                    active BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP WITH TIME ZONE,
                    updated_at TIMESTAMP WITH TIME ZONE
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS employee_invitations (
                    id BIGSERIAL PRIMARY KEY,
                    tenant_id VARCHAR(120) NOT NULL,
                    email VARCHAR(255) NOT NULL,
                    first_name VARCHAR(255) NOT NULL,
                    last_name VARCHAR(255) NOT NULL,
                    country_of_residence VARCHAR(20),
                    phone_number VARCHAR(80),
                    department VARCHAR(255),
                    job_title VARCHAR(255),
                    birth_date DATE,
                    gender VARCHAR(40) NOT NULL DEFAULT 'AUTRES',
                    contract_type VARCHAR(40) NOT NULL DEFAULT 'CDI',
                    hire_date DATE,
                    token_hash VARCHAR(128) NOT NULL,
                    invited_by VARCHAR(255),
                    expires_at TIMESTAMP WITH TIME ZONE,
                    accepted_at TIMESTAMP WITH TIME ZONE,
                    created_at TIMESTAMP WITH TIME ZONE,
                    updated_at TIMESTAMP WITH TIME ZONE,
                    CONSTRAINT uk_employee_invitation_token UNIQUE (token_hash)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS password_reset_tokens (
                    id BIGSERIAL PRIMARY KEY,
                    tenant_id VARCHAR(120) NOT NULL,
                    email VARCHAR(255) NOT NULL,
                    token_hash VARCHAR(128) NOT NULL,
                    expires_at TIMESTAMP WITH TIME ZONE,
                    used_at TIMESTAMP WITH TIME ZONE,
                    created_at TIMESTAMP WITH TIME ZONE,
                    updated_at TIMESTAMP WITH TIME ZONE,
                    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash)
                )
                """);
        jdbcTemplate.execute("ALTER TABLE employees ADD COLUMN IF NOT EXISTS contract_type VARCHAR(40) NOT NULL DEFAULT 'CDI'");
        jdbcTemplate.execute("ALTER TABLE employees ADD COLUMN IF NOT EXISTS birth_date DATE");
        jdbcTemplate.execute("ALTER TABLE employees ADD COLUMN IF NOT EXISTS gender VARCHAR(40) NOT NULL DEFAULT 'AUTRES'");
        jdbcTemplate.execute("ALTER TABLE employee_invitations ADD COLUMN IF NOT EXISTS contract_type VARCHAR(40) NOT NULL DEFAULT 'CDI'");
        jdbcTemplate.execute("ALTER TABLE employee_invitations ADD COLUMN IF NOT EXISTS birth_date DATE");
        jdbcTemplate.execute("ALTER TABLE employee_invitations ADD COLUMN IF NOT EXISTS gender VARCHAR(40) NOT NULL DEFAULT 'AUTRES'");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_employees_tenant ON employees (tenant_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_employee_roles_employee ON employee_roles (employee_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_tenant_workspaces_owner_email ON tenant_workspaces (owner_email)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_employee_invitations_tenant ON employee_invitations (tenant_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_employee_invitations_email ON employee_invitations (tenant_id, email)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_tenant_email ON password_reset_tokens (tenant_id, email)");
    }
}
