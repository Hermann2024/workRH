package com.workrh.telework.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "telework_policies", uniqueConstraints = {
        @UniqueConstraint(name = "uk_policy_tenant_country", columnNames = {"tenantId", "countryCode"})
})
public class TeleworkPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tenantId;
    private String countryCode;
    private int annualFiscalLimitDays;
    private int weeklyCompanyLimitDays;
    private boolean weeklyLimitEnabled = true;
    private boolean active = true;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
