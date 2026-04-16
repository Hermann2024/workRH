package com.workrh.notification.service;

import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.notification.api.dto.SmsNotificationRequest;
import com.workrh.notification.api.dto.SmsNotificationResponse;
import com.workrh.notification.domain.NotificationLog;
import com.workrh.notification.repository.NotificationLogRepository;
import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SmsNotificationService {

    private final NotificationLogRepository notificationLogRepository;
    private final RestClient restClient = RestClient.create();

    @Value("${notification.sms.enabled:true}")
    private boolean smsEnabled;

    @Value("${notification.sms.provider:demo-webhook}")
    private String provider;

    @Value("${notification.sms.from:WorkRH}")
    private String senderName;

    @Value("${notification.sms.webhook-url:}")
    private String webhookUrl;

    @Value("${notification.sms.api-key:}")
    private String apiKey;

    public SmsNotificationService(NotificationLogRepository notificationLogRepository) {
        this.notificationLogRepository = notificationLogRepository;
    }

    public SmsNotificationResponse send(SmsNotificationRequest request) {
        validateRequest(request);
        Instant sentAt = Instant.now();

        if (!smsEnabled) {
            saveLog(request, "SMS_DISABLED", "SMS sending disabled in configuration");
            throw new BadRequestException("SMS sending is disabled in this environment");
        }

        if (webhookUrl == null || webhookUrl.isBlank()) {
            saveLog(request, "SMS_UNAVAILABLE", "SMS provider is not configured in this environment");
            throw new BadRequestException("SMS provider is not configured for this environment");
        }

        try {
            var requestSpec = restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "to", request.phoneNumber(),
                            "message", request.message(),
                            "from", senderName
                    ));
            if (apiKey != null && !apiKey.isBlank()) {
                requestSpec.header("Authorization", "Bearer " + apiKey);
            }
            requestSpec.retrieve().toBodilessEntity();
            saveLog(request, "SMS", "SMS accepted by provider " + provider);
            return new SmsNotificationResponse(provider, "SENT", sentAt, request.phoneNumber());
        } catch (RuntimeException exception) {
            saveLog(request, "SMS_FAILED", "SMS delivery failed: " + exception.getMessage());
            throw new IllegalStateException("Unable to send SMS notification", exception);
        }
    }

    private void validateRequest(SmsNotificationRequest request) {
        if (request == null) {
            throw new BadRequestException("SMS payload is required");
        }
        if (request.phoneNumber() == null || request.phoneNumber().isBlank()) {
            throw new BadRequestException("SMS phone number is required");
        }
        if (request.message() == null || request.message().isBlank()) {
            throw new BadRequestException("SMS message is required");
        }
    }

    private void saveLog(SmsNotificationRequest request, String channel, String payload) {
        NotificationLog log = new NotificationLog();
        log.setTenantId(TenantContext.getTenantId());
        log.setEmployeeId(request.employeeId());
        log.setChannel(channel);
        log.setSubject("SMS notification");
        log.setPayload(payload);
        log.setSentAt(Instant.now());
        notificationLogRepository.save(log);
    }
}
