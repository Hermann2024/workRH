package com.workrh.users.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tenant_workspaces")
public class TenantWorkspace {

    @Id
    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId;

    @Column(nullable = false)
    private String companyName;

    private String ownerEmail;
    private String planCode;
    private Integer seatsPurchased;
    private boolean active = true;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
