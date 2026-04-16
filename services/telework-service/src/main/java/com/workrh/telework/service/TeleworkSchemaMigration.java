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
    }
}
