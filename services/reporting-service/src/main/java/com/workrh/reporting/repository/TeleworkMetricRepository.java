package com.workrh.reporting.repository;

import com.workrh.reporting.domain.TeleworkMetricSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeleworkMetricRepository extends JpaRepository<TeleworkMetricSnapshot, Long> {
    List<TeleworkMetricSnapshot> findAllByTenantIdAndYearAndMonth(String tenantId, int year, int month);
    List<TeleworkMetricSnapshot> findAllByTenantIdAndYearOrderByMonthAsc(String tenantId, int year);
    Optional<TeleworkMetricSnapshot> findByTenantIdAndEmployeeIdAndYearAndMonth(String tenantId, Long employeeId, int year, int month);
    List<TeleworkMetricSnapshot> findAllByTenantIdAndEmployeeId(String tenantId, Long employeeId);
}
