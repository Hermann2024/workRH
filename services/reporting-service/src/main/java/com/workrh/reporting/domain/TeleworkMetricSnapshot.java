package com.workrh.reporting.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "telework_metric_snapshots")
public class TeleworkMetricSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tenantId;
    private Long employeeId;
    private int year;
    private int month;
    private int usedDays;
    private int annualUsedDays;
    private int annualRemainingDays;
    private int weeklyUsedDays;
    private boolean annualFiscalLimitExceeded;
    private boolean weeklyCompanyLimitExceeded;
    private Instant updatedAt = Instant.now();
}
