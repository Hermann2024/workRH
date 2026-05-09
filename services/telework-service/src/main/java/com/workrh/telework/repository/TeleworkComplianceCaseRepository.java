package com.workrh.telework.repository;

import com.workrh.telework.domain.TeleworkComplianceCase;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeleworkComplianceCaseRepository extends JpaRepository<TeleworkComplianceCase, Long> {
    Optional<TeleworkComplianceCase> findByTenantIdAndEmployeeIdAndYearAndMonth(String tenantId, Long employeeId, int year, int month);
    List<TeleworkComplianceCase> findAllByTenantIdAndYearAndMonth(String tenantId, int year, int month);
    Optional<TeleworkComplianceCase> findByIdAndTenantId(Long id, String tenantId);
}
