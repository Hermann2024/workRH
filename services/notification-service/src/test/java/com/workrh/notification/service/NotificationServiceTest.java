package com.workrh.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workrh.common.events.LeaveStatusChangedEvent;
import com.workrh.common.events.ThresholdAlertEvent;
import com.workrh.common.tenant.TenantContext;
import com.workrh.notification.domain.NotificationLog;
import com.workrh.notification.repository.NotificationLogRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class NotificationServiceTest {

    private final NotificationLogRepository notificationLogRepository = Mockito.mock(NotificationLogRepository.class);
    private final InvoiceEmailService invoiceEmailService = Mockito.mock(InvoiceEmailService.class);
    private final NotificationEmailService notificationEmailService = Mockito.mock(NotificationEmailService.class);
    private final NotificationService notificationService = new NotificationService(
            notificationLogRepository,
            invoiceEmailService,
            notificationEmailService
    );

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldLogThresholdAlertWhenAutomationEmailSent() {
        ThresholdAlertEvent event = new ThresholdAlertEvent("tenant-a", 7L, 28, 34, 80, Instant.now());
        when(notificationEmailService.sendThresholdAlert(event)).thenReturn(true);

        notificationService.onAlertEvent(event);

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository).save(captor.capture());
        assertThat(captor.getValue().getChannel()).isEqualTo("EMAIL");
        assertThat(captor.getValue().getSubject()).contains("80%");
    }

    @Test
    void shouldExposePayloadInNotificationFeed() {
        TenantContext.setTenantId("tenant-a");
        NotificationLog log = new NotificationLog();
        log.setId(1L);
        log.setTenantId("tenant-a");
        log.setEmployeeId(3L);
        log.setChannel("EMAIL_SKIPPED");
        log.setSubject("Leave approved");
        log.setPayload("Leave approved from 2026-04-01 to 2026-04-02");
        log.setSentAt(Instant.now());
        when(notificationLogRepository.findAllByTenantIdOrderBySentAtDesc("tenant-a")).thenReturn(List.of(log));

        var response = notificationService.list();

        assertThat(response).singleElement().satisfies(item -> {
            assertThat(item.subject()).isEqualTo("Leave approved");
            assertThat(item.payload()).contains("2026-04-01");
        });
    }

    @Test
    void shouldLogApprovedLeaveAutomation() {
        LeaveStatusChangedEvent event = new LeaveStatusChangedEvent(
                "tenant-a",
                3L,
                11L,
                "APPROVED",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 2),
                Instant.now()
        );
        when(notificationEmailService.sendLeaveApproved(any(LeaveStatusChangedEvent.class))).thenReturn(false);

        notificationService.onLeaveValidated(event);

        verify(notificationLogRepository).save(any(NotificationLog.class));
    }
}
