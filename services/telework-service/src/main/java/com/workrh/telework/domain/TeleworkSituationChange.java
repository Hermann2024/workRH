package com.workrh.telework.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "telework_situation_changes")
public class TeleworkSituationChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tenantId;
    private Long employeeId;
    @Enumerated(EnumType.STRING)
    private TeleworkSituationChangeType type;
    private LocalDate effectiveDate;
    private String previousValue;
    private String newValue;
    private String reason;
    private String recordedBy;
    private Instant recordedAt = Instant.now();
}
