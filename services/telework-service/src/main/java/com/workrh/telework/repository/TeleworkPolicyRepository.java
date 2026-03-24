package com.workrh.telework.repository;

import com.workrh.telework.domain.TeleworkPolicy;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeleworkPolicyRepository extends JpaRepository<TeleworkPolicy, Long> {
    Optional<TeleworkPolicy> findByTenantIdAndCountryCodeAndActiveTrue(String tenantId, String countryCode);
    List<TeleworkPolicy> findAllByTenantId(String tenantId);
    Optional<TeleworkPolicy> findByIdAndTenantId(Long id, String tenantId);
    boolean existsByTenantIdAndCountryCode(String tenantId, String countryCode);
    boolean existsByTenantIdAndCountryCodeAndIdNot(String tenantId, String countryCode, Long id);
}
