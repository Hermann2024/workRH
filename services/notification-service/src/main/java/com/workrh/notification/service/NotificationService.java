package com.workrh.notification.service;

import com.workrh.common.events.LeaveStatusChangedEvent;
import com.workrh.common.events.InvoiceIssuedEvent;
import com.workrh.common.events.ThresholdAlertEvent;
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
    private final InvoiceEmailService invoiceEmailService;
    private final NotificationEmailService notificationEmailService;

    public NotificationService(
            NotificationLogRepository notificationLogRepository,
            InvoiceEmailService invoiceEmailService,
            NotificationEmailService notificationEmailService) {
        this.notificationLogRepository = notificationLogRepository;
        this.invoiceEmailService = invoiceEmailService;
        this.notificationEmailService = notificationEmailService;
    }

    @KafkaListener(topics = "alert-events", groupId = "notification-service")
    public void onAlertEvent(Object event) {
        if (event instanceof ThresholdAlertEvent thresholdAlertEvent) {
            boolean sent = notificationEmailService.sendThresholdAlert(thresholdAlertEvent);
            save(
                    thresholdAlertEvent.tenantId(),
                    thresholdAlertEvent.employeeId(),
                    sent ? "EMAIL" : "EMAIL_SKIPPED",
                    "Telework threshold alert " + thresholdAlertEvent.alertStagePercent() + "%",
                    "Employee %d reached %d/%d telework days (%d%% of annual threshold)".formatted(
                            thresholdAlertEvent.employeeId(),
                            thresholdAlertEvent.annualUsedDays(),
                            thresholdAlertEvent.annualLimit(),
                            thresholdAlertEvent.alertStagePercent()
                    )
            );
            return;
        }
        if (!(event instanceof ThresholdExceededEvent thresholdExceededEvent)) {
            return;
        }
        boolean sent = notificationEmailService.sendThresholdExceeded(thresholdExceededEvent);
        save(
                thresholdExceededEvent.tenantId(),
                thresholdExceededEvent.employeeId(),
                sent ? "EMAIL" : "EMAIL_SKIPPED",
                "Telework threshold exceeded",
                "Employee %d exceeded %d/%d telework days".formatted(
                        thresholdExceededEvent.employeeId(),
                        thresholdExceededEvent.annualUsedDays(),
                        thresholdExceededEvent.annualLimit()
                )
        );
    }

    @KafkaListener(topics = "leave-events", groupId = "notification-service")
    public void onLeaveValidated(LeaveStatusChangedEvent event) {
        if ("APPROVED".equals(event.status())) {
            boolean sent = notificationEmailService.sendLeaveApproved(event);
            save(
                    event.tenantId(),
                    event.employeeId(),
                    sent ? "EMAIL" : "EMAIL_SKIPPED",
                    "Leave approved",
                    "Leave approved from %s to %s".formatted(event.startDate(), event.endDate())
            );
        }
    }

    @KafkaListener(topics = "billing-events", groupId = "notification-service")
    public void onInvoiceIssued(InvoiceIssuedEvent event) {
        try {
            boolean sent = invoiceEmailService.sendInvoice(event);
            save(
                    event.tenantId(),
                    null,
                    sent ? "EMAIL" : "EMAIL_SKIPPED",
                    "Invoice " + event.invoiceNumber(),
                    "Invoice %s for %s prepared for %s via %s".formatted(
                            event.invoiceNumber(),
                            event.applicationName(),
                            event.customerEmail(),
                            sent ? "SMTP" : "local fallback"
                    )
            );
        } catch (RuntimeException exception) {
            save(
                    event.tenantId(),
                    null,
                    "EMAIL_FAILED",
                    "Invoice " + event.invoiceNumber(),
                    "Invoice delivery failed for %s: %s".formatted(event.customerEmail(), exception.getMessage())
            );
            throw exception;
        }
    }

    public List<NotificationResponseDto> list() {
        return notificationLogRepository.findAllByTenantIdOrderBySentAtDesc(TenantContext.getTenantId()).stream()
                .map(log -> new NotificationResponseDto(
                        log.getId(),
                        log.getEmployeeId(),
                        log.getChannel(),
                        log.getSubject(),
                        log.getPayload(),
                        log.getSentAt()
                ))
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
