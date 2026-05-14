CREATE TABLE IF NOT EXISTS telework_metric_snapshots (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    employee_id BIGINT NOT NULL,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    used_days INTEGER NOT NULL DEFAULT 0,
    annual_used_days INTEGER NOT NULL DEFAULT 0,
    annual_fiscal_limit_days INTEGER NOT NULL DEFAULT 34,
    annual_remaining_days INTEGER NOT NULL DEFAULT 0,
    weekly_used_days INTEGER NOT NULL DEFAULT 0,
    annual_fiscal_limit_exceeded BOOLEAN NOT NULL DEFAULT FALSE,
    weekly_company_limit_exceeded BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_metric_snapshots_tenant
    ON telework_metric_snapshots (tenant_id);

CREATE INDEX IF NOT EXISTS idx_metric_snapshots_tenant_employee
    ON telework_metric_snapshots (tenant_id, employee_id);

CREATE INDEX IF NOT EXISTS idx_metric_snapshots_tenant_period
    ON telework_metric_snapshots (tenant_id, year, month);

CREATE INDEX IF NOT EXISTS idx_metric_snapshots_tenant_employee_period
    ON telework_metric_snapshots (tenant_id, employee_id, year, month);

CREATE TABLE IF NOT EXISTS hr_connector_configurations (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    auth_type VARCHAR(50) NOT NULL,
    sync_mode VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    client_id VARCHAR(255),
    client_secret VARCHAR(1000),
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
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_hr_connectors_tenant_provider
    ON hr_connector_configurations (tenant_id, provider);

CREATE TABLE IF NOT EXISTS hr_connector_field_mappings (
    configuration_id BIGINT NOT NULL REFERENCES hr_connector_configurations(id) ON DELETE CASCADE,
    workrh_field VARCHAR(255) NOT NULL,
    provider_field VARCHAR(500) NOT NULL,
    PRIMARY KEY (configuration_id, workrh_field)
);

CREATE TABLE IF NOT EXISTS hr_connector_api_settings (
    configuration_id BIGINT NOT NULL REFERENCES hr_connector_configurations(id) ON DELETE CASCADE,
    setting_key VARCHAR(255) NOT NULL,
    setting_value VARCHAR(1000) NOT NULL,
    PRIMARY KEY (configuration_id, setting_key)
);

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
);

CREATE INDEX IF NOT EXISTS idx_hr_connector_sync_runs_tenant_provider
    ON hr_connector_sync_runs (tenant_id, provider, started_at DESC);
