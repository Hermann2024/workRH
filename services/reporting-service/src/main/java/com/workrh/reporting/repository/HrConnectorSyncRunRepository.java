package com.workrh.reporting.repository;

import com.workrh.reporting.domain.HrConnectorSyncRun;
import com.workrh.reporting.domain.HrProvider;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HrConnectorSyncRunRepository extends JpaRepository<HrConnectorSyncRun, Long> {
    List<HrConnectorSyncRun> findTop10ByTenantIdAndProviderOrderByStartedAtDesc(String tenantId, HrProvider provider);
}
