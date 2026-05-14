package com.workrh.notification.api.dto;

import java.time.Instant;

public record SupportTicketResponse(
        Long id,
        String tenantId,
        String category,
        String priority,
        String status,
        String requesterName,
        String requesterEmail,
        String phoneNumber,
        String subject,
        String message,
        String resolutionMessage,
        String resolvedBy,
        Instant slaDueAt,
        Instant resolvedAt,
        Instant createdAt,
        boolean slaBreached
) {
}
