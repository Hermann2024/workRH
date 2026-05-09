package com.workrh.telework.repository;

import com.workrh.telework.domain.TeleworkComplianceAuditEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeleworkComplianceAuditEntryRepository extends JpaRepository<TeleworkComplianceAuditEntry, Long> {
    List<TeleworkComplianceAuditEntry> findAllByTenantIdAndComplianceCaseIdOrderByCreatedAtDesc(String tenantId, Long complianceCaseId);
    List<TeleworkComplianceAuditEntry> findTop100ByTenantIdOrderByCreatedAtDesc(String tenantId);
}
