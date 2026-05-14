package com.workrh.users.repository;

import com.workrh.users.domain.EmployeeInvitation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeInvitationRepository extends JpaRepository<EmployeeInvitation, Long> {
    List<EmployeeInvitation> findAllByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<EmployeeInvitation> findByTokenHash(String tokenHash);
    boolean existsByEmailAndTenantIdAndAcceptedAtIsNull(String email, String tenantId);
}
