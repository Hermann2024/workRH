package com.workrh.leave.service;

import com.workrh.leave.domain.LeaveType;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(5)
public class LeaveSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public LeaveSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        String allowedTypes = Arrays.stream(LeaveType.values())
                .map(LeaveType::name)
                .map(value -> "'" + value + "'")
                .collect(Collectors.joining(", "));

        jdbcTemplate.execute("ALTER TABLE leave_requests DROP CONSTRAINT IF EXISTS leave_requests_type_check");
        jdbcTemplate.execute(
                "ALTER TABLE leave_requests ADD CONSTRAINT leave_requests_type_check CHECK (type IN (" + allowedTypes + "))"
        );
    }
}
