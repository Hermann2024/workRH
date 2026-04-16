package com.workrh.notification.api.dto;

import java.time.Instant;

public record SupportTicketResponse(
        Long id,
        String category,
        String priority,
        String status,
        String requesterName,
        String requesterEmail,
        String phoneNumber,
        String subject,
        String message,
        Instant slaDueAt,
        Instant createdAt,
        boolean slaBreached
) {
}
