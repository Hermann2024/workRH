package com.workrh.notification.api.dto;

import java.time.Instant;

public record NotificationResponseDto(Long id, Long employeeId, String channel, String subject, String payload, Instant sentAt) {
}
