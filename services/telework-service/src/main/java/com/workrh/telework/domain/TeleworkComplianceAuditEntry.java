package com.workrh.telework.domain;

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
@Table(name = "telework_compliance_audit_entries")
public class TeleworkComplianceAuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tenantId;
    private Long complianceCaseId;
    private Long employeeId;
    private String action;
    private String actor;
    private String beforeValue;
    private String afterValue;
    private Instant createdAt = Instant.now();
}
