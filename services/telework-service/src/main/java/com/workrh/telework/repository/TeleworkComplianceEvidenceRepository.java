package com.workrh.telework.repository;

import com.workrh.telework.domain.TeleworkComplianceEvidence;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeleworkComplianceEvidenceRepository extends JpaRepository<TeleworkComplianceEvidence, Long> {
    List<TeleworkComplianceEvidence> findAllByTenantIdAndComplianceCaseIdOrderByUploadedAtDesc(String tenantId, Long complianceCaseId);
}
