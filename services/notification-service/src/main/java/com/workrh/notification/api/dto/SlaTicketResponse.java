package com.workrh.notification.api.dto;

import java.time.Instant;

public record SlaTicketResponse(
        Long id,
        String subject,
        String priority,
        String status,
        Instant slaDueAt,
        boolean breached,
        long remainingMinutes
) {
}
