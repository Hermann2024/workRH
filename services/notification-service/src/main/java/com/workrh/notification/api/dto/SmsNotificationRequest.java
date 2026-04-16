package com.workrh.notification.api.dto;

public record SmsNotificationRequest(
        Long employeeId,
        String phoneNumber,
        String message
) {
}
