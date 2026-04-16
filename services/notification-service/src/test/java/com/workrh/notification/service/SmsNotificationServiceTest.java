package com.workrh.notification.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.notification.api.dto.SmsNotificationRequest;
import com.workrh.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

class SmsNotificationServiceTest {

    private final NotificationLogRepository notificationLogRepository = Mockito.mock(NotificationLogRepository.class);
    private final SmsNotificationService smsNotificationService = new SmsNotificationService(notificationLogRepository);

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldRejectSmsWhenNoWebhookConfigured() {
        TenantContext.setTenantId("tenant-a");
        ReflectionTestUtils.setField(smsNotificationService, "smsEnabled", true);
        ReflectionTestUtils.setField(smsNotificationService, "provider", "demo-webhook");
        ReflectionTestUtils.setField(smsNotificationService, "senderName", "WorkRH");
        ReflectionTestUtils.setField(smsNotificationService, "webhookUrl", "");

        assertThatThrownBy(() -> smsNotificationService.send(new SmsNotificationRequest(7L, "+33600000000", "Test SMS")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("SMS provider is not configured for this environment");

        verify(notificationLogRepository).save(any());
    }
}
