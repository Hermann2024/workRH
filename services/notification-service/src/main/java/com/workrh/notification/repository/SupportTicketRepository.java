package com.workrh.notification.repository;

import com.workrh.notification.domain.SupportTicket;
import com.workrh.notification.domain.SupportTicketStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findAllByTenantIdOrderByCreatedAtDesc(String tenantId);

    List<SupportTicket> findAllByTenantIdAndStatusInOrderByCreatedAtDesc(String tenantId, Collection<SupportTicketStatus> statuses);
}
