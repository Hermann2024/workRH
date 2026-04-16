package com.workrh.notification.repository;

import com.workrh.notification.domain.NotificationLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findAllByTenantId(String tenantId);
    List<NotificationLog> findAllByTenantIdOrderBySentAtDesc(String tenantId);
}
