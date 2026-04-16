package com.workrh.notification.api.dto;

import java.time.Instant;

public record SmsNotificationResponse(
        String provider,
        String status,
        Instant sentAt,
        String phoneNumber
) {
}
