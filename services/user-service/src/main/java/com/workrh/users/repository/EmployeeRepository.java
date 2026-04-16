package com.workrh.users.repository;

import com.workrh.users.domain.Employee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmailAndTenantId(String email, String tenantId);
    List<Employee> findAllByTenantId(String tenantId);
    Optional<Employee> findByIdAndTenantId(Long id, String tenantId);
    boolean existsByTenantId(String tenantId);
    boolean existsByEmailAndTenantId(String email, String tenantId);
    boolean existsByEmailAndTenantIdAndIdNot(String email, String tenantId, Long id);
}
