package com.workrh.reporting.repository;

import com.workrh.reporting.domain.HrConnectorConfiguration;
import com.workrh.reporting.domain.HrProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HrConnectorConfigurationRepository extends JpaRepository<HrConnectorConfiguration, Long> {
    List<HrConnectorConfiguration> findAllByTenantIdOrderByProviderAsc(String tenantId);
    Optional<HrConnectorConfiguration> findByTenantIdAndProvider(String tenantId, HrProvider provider);
}
