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
@Table(name = "telework_compliance_evidence")
public class TeleworkComplianceEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tenantId;
    private Long complianceCaseId;
    private String evidenceType;
    private String label;
    private String reference;
    private String fileUrl;
    private String uploadedBy;
    private Instant uploadedAt = Instant.now();
}
