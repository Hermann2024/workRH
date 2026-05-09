package com.workrh.telework.repository;

import com.workrh.telework.domain.TeleworkSituationChange;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeleworkSituationChangeRepository extends JpaRepository<TeleworkSituationChange, Long> {
    List<TeleworkSituationChange> findTop50ByTenantIdAndEmployeeIdOrderByRecordedAtDesc(String tenantId, Long employeeId);
    List<TeleworkSituationChange> findTop100ByTenantIdOrderByRecordedAtDesc(String tenantId);
}
