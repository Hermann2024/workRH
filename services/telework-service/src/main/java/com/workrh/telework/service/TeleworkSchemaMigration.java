package com.workrh.telework.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(6)
public class TeleworkSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public TeleworkSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("ALTER TABLE telework_declarations ADD COLUMN IF NOT EXISTS total_work_minutes integer");
        jdbcTemplate.execute("ALTER TABLE telework_declarations ADD COLUMN IF NOT EXISTS residence_telework_minutes integer");
        jdbcTemplate.execute("ALTER TABLE telework_declarations ADD COLUMN IF NOT EXISTS residence_non_telework_minutes integer");
        jdbcTemplate.execute("ALTER TABLE telework_declarations ADD COLUMN IF NOT EXISTS other_foreign_work_minutes integer");
        jdbcTemplate.execute("ALTER TABLE telework_declarations ADD COLUMN IF NOT EXISTS other_foreign_country_code varchar(32)");
        jdbcTemplate.execute("ALTER TABLE telework_declarations ADD COLUMN IF NOT EXISTS connected_to_employer_infrastructure boolean");

        jdbcTemplate.execute("UPDATE telework_declarations SET total_work_minutes = COALESCE(total_work_minutes, 480)");
        jdbcTemplate.execute("UPDATE telework_declarations SET residence_telework_minutes = COALESCE(residence_telework_minutes, 480)");
        jdbcTemplate.execute("UPDATE telework_declarations SET residence_non_telework_minutes = COALESCE(residence_non_telework_minutes, 0)");
        jdbcTemplate.execute("UPDATE telework_declarations SET other_foreign_work_minutes = COALESCE(other_foreign_work_minutes, 0)");
        jdbcTemplate.execute("UPDATE telework_declarations SET connected_to_employer_infrastructure = COALESCE(connected_to_employer_infrastructure, true)");

        jdbcTemplate.execute("ALTER TABLE telework_policies ADD COLUMN IF NOT EXISTS standard_daily_work_minutes integer");
        jdbcTemplate.execute("UPDATE telework_policies SET standard_daily_work_minutes = COALESCE(standard_daily_work_minutes, 480)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_telework_declarations_tenant ON telework_declarations (tenant_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_telework_declarations_tenant_employee ON telework_declarations (tenant_id, employee_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_telework_declarations_tenant_work_date ON telework_declarations (tenant_id, work_date)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_telework_declarations_tenant_employee_work_date ON telework_declarations (tenant_id, employee_id, work_date)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_telework_policies_tenant ON telework_policies (tenant_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_telework_policies_tenant_country ON telework_policies (tenant_id, country_code)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_exclusion_periods_tenant_employee ON exclusion_periods (tenant_id, employee_id)");

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS telework_compliance_cases (
                    id bigserial PRIMARY KEY,
                    tenant_id varchar(255),
                    employee_id bigint,
                    year integer,
                    month integer,
                    country_code varchar(32),
                    employment_sector varchar(32),
                    status varchar(64),
                    ccssa1status varchar(64),
                    telework_agreement_status varchar(64),
                    equipment_status varchar(64),
                    health_safety_status varchar(64),
                    accident_coverage_status varchar(64),
                    data_protection_status varchar(64),
                    residence_country_rules_status varchar(64),
                    legal_watch_status varchar(64),
                    a1submitted_at date,
                    a1valid_until date,
                    legal_sources_reviewed_at date,
                    next_legal_review_at date,
                    legal_sources_version varchar(255),
                    notes text,
                    validated_by varchar(255),
                    validated_at timestamp,
                    created_at timestamp,
                    updated_at timestamp
                )
                """);
        jdbcTemplate.execute("CREATE UNIQUE INDEX IF NOT EXISTS uk_compliance_case_period_employee ON telework_compliance_cases (tenant_id, employee_id, year, month)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_compliance_cases_tenant_period ON telework_compliance_cases (tenant_id, year, month)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_compliance_cases_tenant_employee ON telework_compliance_cases (tenant_id, employee_id)");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS telework_compliance_evidence (
                    id bigserial PRIMARY KEY,
                    tenant_id varchar(255),
                    compliance_case_id bigint,
                    evidence_type varchar(255),
                    label varchar(255),
                    reference varchar(500),
                    file_url varchar(1000),
                    uploaded_by varchar(255),
                    uploaded_at timestamp
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_compliance_evidence_tenant_case ON telework_compliance_evidence (tenant_id, compliance_case_id)");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS telework_situation_changes (
                    id bigserial PRIMARY KEY,
                    tenant_id varchar(255),
                    employee_id bigint,
                    type varchar(64),
                    effective_date date,
                    previous_value varchar(500),
                    new_value varchar(500),
                    reason text,
                    recorded_by varchar(255),
                    recorded_at timestamp
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_situation_changes_tenant_employee ON telework_situation_changes (tenant_id, employee_id)");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS telework_compliance_audit_entries (
                    id bigserial PRIMARY KEY,
                    tenant_id varchar(255),
                    compliance_case_id bigint,
                    employee_id bigint,
                    action varchar(255),
                    actor varchar(255),
                    before_value text,
                    after_value text,
                    created_at timestamp
                )
                """);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_compliance_audit_tenant_case ON telework_compliance_audit_entries (tenant_id, compliance_case_id)");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_compliance_audit_tenant_created ON telework_compliance_audit_entries (tenant_id, created_at)");
    }
}
