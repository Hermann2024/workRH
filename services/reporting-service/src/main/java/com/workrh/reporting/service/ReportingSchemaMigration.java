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
        jdbcTemplate.execute("ALTER TABLE telework_metric_snapshots ADD COLUMN IF NOT EXISTS annual_fiscal_limit_days INTEGER NOT NULL DEFAULT 34");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_metric_snapshots_tenant ON telework_metric_snapshots (tenant_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_metric_snapshots_tenant_employee ON telework_metric_snapshots (tenant_id, employee_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_metric_snapshots_tenant_period ON telework_metric_snapshots (tenant_id, year, month)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_metric_snapshots_tenant_employee_period ON telework_metric_snapshots (tenant_id, employee_id, year, month)");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS hr_connector_configurations (
                    id BIGSERIAL PRIMARY KEY,
                    tenant_id VARCHAR(255) NOT NULL,
                    provider VARCHAR(50) NOT NULL,
                    auth_type VARCHAR(50) NOT NULL,
                    sync_mode VARCHAR(50) NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    enabled BOOLEAN NOT NULL DEFAULT FALSE,
                    client_id VARCHAR(255),
                    client_secret VARCHAR(255),
                    api_base_url VARCHAR(500),
                    authorization_url VARCHAR(500),
                    token_url VARCHAR(500),
                    redirect_uri VARCHAR(500),
                    scopes VARCHAR(500),
                    access_token TEXT,
                    refresh_token TEXT,
                    token_expires_at TIMESTAMP WITH TIME ZONE,
                    last_sync_at TIMESTAMP WITH TIME ZONE,
                    last_sync_status VARCHAR(50),
                    last_sync_message VARCHAR(1000),
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS hr_connector_field_mappings (
                    configuration_id BIGINT NOT NULL REFERENCES hr_connector_configurations(id) ON DELETE CASCADE,
                    workrh_field VARCHAR(255) NOT NULL,
                    provider_field VARCHAR(500) NOT NULL,
                    PRIMARY KEY (configuration_id, workrh_field)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS hr_connector_sync_runs (
                    id BIGSERIAL PRIMARY KEY,
                    tenant_id VARCHAR(255) NOT NULL,
                    provider VARCHAR(50) NOT NULL,
                    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                    finished_at TIMESTAMP WITH TIME ZONE,
                    status VARCHAR(50),
                    message VARCHAR(1000),
                    imported_records INTEGER NOT NULL DEFAULT 0,
                    skipped_records INTEGER NOT NULL DEFAULT 0
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS hr_connector_api_settings (
                    configuration_id BIGINT NOT NULL REFERENCES hr_connector_configurations(id) ON DELETE CASCADE,
                    setting_key VARCHAR(255) NOT NULL,
                    setting_value VARCHAR(1000) NOT NULL,
                    PRIMARY KEY (configuration_id, setting_key)
                )
                """);
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_hr_connectors_tenant_provider ON hr_connector_configurations (tenant_id, provider)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_hr_connector_sync_runs_tenant_provider ON hr_connector_sync_runs (tenant_id, provider, started_at DESC)");
    }
}
