package com.workrh.sickness.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sickness_records")
public class SicknessRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tenantId;
    private Long employeeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String comment;
    private String evidenceFileName;
    private String evidenceContentType;
    @Column(columnDefinition = "bytea")
    private byte[] evidenceContent;
    private Instant evidenceUploadedAt;
    private String evidenceUploadedBy;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
