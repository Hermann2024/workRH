package com.workrh.telework.repository;

import com.workrh.telework.domain.ExclusionPeriod;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExclusionPeriodRepository extends JpaRepository<ExclusionPeriod, Long> {
    List<ExclusionPeriod> findAllByTenantIdAndEmployeeId(String tenantId, Long employeeId);
}
