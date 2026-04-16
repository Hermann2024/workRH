package com.workrh.notification.api.dto;

public record SupportTicketRequest(
        String requesterName,
        String requesterEmail,
        String phoneNumber,
        String subject,
        String message
) {
}
