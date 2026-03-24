package com.workrh.notification.service;

import com.workrh.common.events.LeaveStatusChangedEvent;
import com.workrh.common.events.ThresholdExceededEvent;
import com.workrh.common.tenant.TenantContext;
import com.workrh.notification.api.dto.NotificationResponseDto;
import com.workrh.notification.domain.NotificationLog;
import com.workrh.notification.repository.NotificationLogRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationLogRepository notificationLogRepository;

    public NotificationService(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    @KafkaListener(topics = "alert-events", groupId = "notification-service")
    public void onThresholdExceeded(ThresholdExceededEvent event) {
        save(event.tenantId(), event.employeeId(), "EMAIL", "Telework threshold exceeded",
                "Employee %d exceeded %d/%d telework days".formatted(event.employeeId(), event.annualUsedDays(), event.annualLimit()));
    }

    @KafkaListener(topics = "leave-events", groupId = "notification-service")
    public void onLeaveValidated(LeaveStatusChangedEvent event) {
        if ("APPROVED".equals(event.status())) {
            save(event.tenantId(), event.employeeId(), "EMAIL", "Leave approved",
                    "Leave approved from %s to %s".formatted(event.startDate(), event.endDate()));
        }
    }

    public List<NotificationResponseDto> list() {
        return notificationLogRepository.findAllByTenantId(TenantContext.getTenantId()).stream()
                .map(log -> new NotificationResponseDto(log.getId(), log.getEmployeeId(), log.getChannel(), log.getSubject(), log.getSentAt()))
                .toList();
    }

    private void save(String tenantId, Long employeeId, String channel, String subject, String payload) {
        NotificationLog log = new NotificationLog();
        log.setTenantId(tenantId);
        log.setEmployeeId(employeeId);
        log.setChannel(channel);
        log.setSubject(subject);
        log.setPayload(payload);
        log.setSentAt(Instant.now());
        notificationLogRepository.save(log);
    }
}
