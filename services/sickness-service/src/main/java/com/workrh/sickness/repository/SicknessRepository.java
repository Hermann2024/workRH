package com.workrh.sickness.repository;

import com.workrh.sickness.domain.SicknessRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SicknessRepository extends JpaRepository<SicknessRecord, Long> {
    List<SicknessRecord> findAllByTenantId(String tenantId);
    List<SicknessRecord> findAllByTenantIdAndEmployeeId(String tenantId, Long employeeId);
    Optional<SicknessRecord> findByIdAndTenantId(Long id, String tenantId);
}
