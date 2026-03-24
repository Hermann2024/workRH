package com.workrh.leave.repository;

import com.workrh.leave.domain.LeaveRequestEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveRepository extends JpaRepository<LeaveRequestEntity, Long> {
    List<LeaveRequestEntity> findAllByTenantId(String tenantId);
    List<LeaveRequestEntity> findAllByTenantIdAndEmployeeId(String tenantId, Long employeeId);
    Optional<LeaveRequestEntity> findByIdAndTenantId(Long id, String tenantId);
}
