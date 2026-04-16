package com.workrh.notification.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "support_tickets")
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tenantId;
    private String requesterName;
    private String requesterEmail;
    private String phoneNumber;
    private String subject;
    private String message;

    @Enumerated(EnumType.STRING)
    private SupportTicketCategory category;

    @Enumerated(EnumType.STRING)
    private SupportTicketPriority priority;

    @Enumerated(EnumType.STRING)
    private SupportTicketStatus status;

    private Instant slaDueAt;
    private Instant resolvedAt;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
