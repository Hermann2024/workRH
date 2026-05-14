package com.workrh.reporting.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "hr_connector_sync_runs")
public class HrConnectorSyncRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String tenantId;

    @Enumerated(EnumType.STRING)
    private HrProvider provider;

    private Instant startedAt = Instant.now();
    private Instant finishedAt;
    private String status;

    @Column(length = 1000)
    private String message;

    private int importedRecords;
    private int skippedRecords;
}
