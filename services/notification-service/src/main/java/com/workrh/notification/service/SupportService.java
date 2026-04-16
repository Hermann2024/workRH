package com.workrh.notification.service;

import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.notification.api.dto.SlaTicketResponse;
import com.workrh.notification.api.dto.SupportTicketRequest;
import com.workrh.notification.api.dto.SupportTicketResponse;
import com.workrh.notification.domain.NotificationLog;
import com.workrh.notification.domain.SupportTicket;
import com.workrh.notification.domain.SupportTicketCategory;
import com.workrh.notification.domain.SupportTicketPriority;
import com.workrh.notification.domain.SupportTicketStatus;
import com.workrh.notification.repository.NotificationLogRepository;
import com.workrh.notification.repository.SupportTicketRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SupportService {

    private final SupportTicketRepository supportTicketRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final SupportEmailService supportEmailService;

    @Value("${notification.support.sla-hours-priority:4}")
    private long prioritySlaHours;

    @Value("${notification.support.sla-hours-onboarding:24}")
    private long onboardingSlaHours;

    public SupportService(
            SupportTicketRepository supportTicketRepository,
            NotificationLogRepository notificationLogRepository,
            SupportEmailService supportEmailService) {
        this.supportTicketRepository = supportTicketRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.supportEmailService = supportEmailService;
    }

    public SupportTicketResponse createStandardTicket(SupportTicketRequest request) {
        return toResponse(createTicket(request, SupportTicketCategory.EMAIL_SUPPORT, SupportTicketPriority.STANDARD, null));
    }

    public SupportTicketResponse createPriorityTicket(SupportTicketRequest request) {
        Instant slaDueAt = Instant.now().plus(Duration.ofHours(prioritySlaHours));
        return toResponse(createTicket(request, SupportTicketCategory.PRIORITY_SUPPORT, SupportTicketPriority.PRIORITY, slaDueAt));
    }

    public SupportTicketResponse createOnboardingTicket(SupportTicketRequest request) {
        Instant slaDueAt = Instant.now().plus(Duration.ofHours(onboardingSlaHours));
        return toResponse(createTicket(request, SupportTicketCategory.ONBOARDING_SUPPORT, SupportTicketPriority.PRIORITY, slaDueAt));
    }

    public SupportTicketResponse createSsoTicket(SupportTicketRequest request) {
        return toResponse(createManagedEnterpriseTicket(request, SupportTicketCategory.SSO_SETUP));
    }

    public SupportTicketResponse createSecurityTicket(SupportTicketRequest request) {
        return toResponse(createManagedEnterpriseTicket(request, SupportTicketCategory.SECURITY_REVIEW));
    }

    public SupportTicketResponse createHostingTicket(SupportTicketRequest request) {
        return toResponse(createManagedEnterpriseTicket(request, SupportTicketCategory.DEDICATED_HOSTING));
    }

    public SupportTicketResponse createCustomDevelopmentTicket(SupportTicketRequest request) {
        return toResponse(createManagedEnterpriseTicket(request, SupportTicketCategory.CUSTOM_DEVELOPMENT));
    }

    public SupportTicketResponse createIntegrationTicket(SupportTicketRequest request) {
        return toResponse(createManagedEnterpriseTicket(request, SupportTicketCategory.INTEGRATION_SUPPORT));
    }

    public List<SupportTicketResponse> listTickets() {
        Instant now = Instant.now();
        return supportTicketRepository.findAllByTenantIdOrderByCreatedAtDesc(TenantContext.getTenantId()).stream()
                .map(ticket -> toResponse(ticket, now))
                .toList();
    }

    public List<SlaTicketResponse> listSlaTickets() {
        Instant now = Instant.now();
        return supportTicketRepository.findAllByTenantIdAndStatusInOrderByCreatedAtDesc(
                        TenantContext.getTenantId(),
                        Set.of(SupportTicketStatus.OPEN, SupportTicketStatus.IN_PROGRESS)
                ).stream()
                .filter(ticket -> ticket.getSlaDueAt() != null)
                .map(ticket -> toSlaResponse(ticket, now))
                .toList();
    }

    private SupportTicket createTicket(
            SupportTicketRequest request,
            SupportTicketCategory category,
            SupportTicketPriority priority,
            Instant slaDueAt) {
        validateRequest(request);

        SupportTicket ticket = new SupportTicket();
        ticket.setTenantId(TenantContext.getTenantId());
        ticket.setRequesterName(trim(request.requesterName()));
        ticket.setRequesterEmail(trim(request.requesterEmail()));
        ticket.setPhoneNumber(trim(request.phoneNumber()));
        ticket.setSubject(trim(request.subject()));
        ticket.setMessage(trim(request.message()));
        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setSlaDueAt(slaDueAt);
        ticket.setCreatedAt(Instant.now());
        ticket.setUpdatedAt(ticket.getCreatedAt());

        SupportTicket saved = supportTicketRepository.save(ticket);
        boolean acknowledgementSent = supportEmailService.sendAcknowledgement(saved);

        saveLog(
                saved.getTenantId(),
                "SUPPORT",
                "Support ticket #" + saved.getId(),
                "%s ticket created with priority %s".formatted(saved.getCategory().name(), saved.getPriority().name())
        );
        saveLog(
                saved.getTenantId(),
                acknowledgementSent ? "EMAIL" : "EMAIL_SKIPPED",
                "Support acknowledgement #" + saved.getId(),
                "Acknowledgement for support ticket #%d to %s".formatted(saved.getId(), defaultValue(saved.getRequesterEmail(), "no-email"))
        );
        return saved;
    }

    private SupportTicket createManagedEnterpriseTicket(SupportTicketRequest request, SupportTicketCategory category) {
        Instant slaDueAt = Instant.now().plus(Duration.ofHours(prioritySlaHours));
        return createTicket(request, category, SupportTicketPriority.PRIORITY, slaDueAt);
    }

    private void validateRequest(SupportTicketRequest request) {
        if (request == null) {
            throw new BadRequestException("Support ticket payload is required");
        }
        if (isBlank(request.subject())) {
            throw new BadRequestException("Support ticket subject is required");
        }
        if (isBlank(request.message())) {
            throw new BadRequestException("Support ticket message is required");
        }
        if (isBlank(request.requesterEmail())) {
            throw new BadRequestException("Support requester email is required");
        }
    }

    private SupportTicketResponse toResponse(SupportTicket ticket) {
        return toResponse(ticket, Instant.now());
    }

    private SupportTicketResponse toResponse(SupportTicket ticket, Instant now) {
        return new SupportTicketResponse(
                ticket.getId(),
                ticket.getCategory().name(),
                ticket.getPriority().name(),
                ticket.getStatus().name(),
                ticket.getRequesterName(),
                ticket.getRequesterEmail(),
                ticket.getPhoneNumber(),
                ticket.getSubject(),
                ticket.getMessage(),
                ticket.getSlaDueAt(),
                ticket.getCreatedAt(),
                ticket.getSlaDueAt() != null && now.isAfter(ticket.getSlaDueAt())
        );
    }

    private SlaTicketResponse toSlaResponse(SupportTicket ticket, Instant now) {
        long remainingMinutes = Duration.between(now, ticket.getSlaDueAt()).toMinutes();
        return new SlaTicketResponse(
                ticket.getId(),
                ticket.getSubject(),
                ticket.getPriority().name(),
                ticket.getStatus().name(),
                ticket.getSlaDueAt(),
                now.isAfter(ticket.getSlaDueAt()),
                remainingMinutes
        );
    }

    private void saveLog(String tenantId, String channel, String subject, String payload) {
        NotificationLog log = new NotificationLog();
        log.setTenantId(tenantId);
        log.setChannel(channel);
        log.setSubject(subject);
        log.setPayload(payload);
        log.setSentAt(Instant.now());
        notificationLogRepository.save(log);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String defaultValue(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }
}
