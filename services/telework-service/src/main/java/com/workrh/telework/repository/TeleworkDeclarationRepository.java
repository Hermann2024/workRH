package com.workrh.telework.repository;

import com.workrh.telework.domain.TeleworkDeclaration;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeleworkDeclarationRepository extends JpaRepository<TeleworkDeclaration, Long> {
    List<TeleworkDeclaration> findAllByTenantIdAndEmployeeId(String tenantId, Long employeeId);
    List<TeleworkDeclaration> findAllByTenantIdAndWorkDateBetween(String tenantId, LocalDate startDate, LocalDate endDate);
    long countByTenantIdAndEmployeeIdAndWorkDateBetween(String tenantId, Long employeeId, LocalDate startDate, LocalDate endDate);
    boolean existsByTenantIdAndEmployeeIdAndWorkDate(String tenantId, Long employeeId, LocalDate workDate);
}
