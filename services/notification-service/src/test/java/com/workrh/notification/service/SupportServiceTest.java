package com.workrh.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.workrh.common.tenant.TenantContext;
import com.workrh.notification.api.dto.SupportTicketRequest;
import com.workrh.notification.domain.NotificationLog;
import com.workrh.notification.domain.SupportTicket;
import com.workrh.notification.domain.SupportTicketCategory;
import com.workrh.notification.domain.SupportTicketPriority;
import com.workrh.notification.domain.SupportTicketStatus;
import com.workrh.notification.repository.NotificationLogRepository;
import com.workrh.notification.repository.SupportTicketRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class SupportServiceTest {

    private final SupportTicketRepository supportTicketRepository = Mockito.mock(SupportTicketRepository.class);
    private final NotificationLogRepository notificationLogRepository = Mockito.mock(NotificationLogRepository.class);
    private final SupportEmailService supportEmailService = Mockito.mock(SupportEmailService.class);
    private final SupportService supportService = new SupportService(
            supportTicketRepository,
            notificationLogRepository,
            supportEmailService
    );

    @AfterEach
    void cleanup() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCreatePriorityTicketWithSla() {
        TenantContext.setTenantId("tenant-a");
        ReflectionTestUtils.setField(supportService, "prioritySlaHours", 4L);
        when(supportEmailService.sendAcknowledgement(any(SupportTicket.class))).thenReturn(false);
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> {
            SupportTicket ticket = invocation.getArgument(0);
            ticket.setId(15L);
            ticket.setCreatedAt(Instant.now());
            return ticket;
        });

        var response = supportService.createPriorityTicket(new SupportTicketRequest(
                "Alice",
                "alice@company.com",
                "+352000000",
                "Blocage paiement",
                "Le checkout echoue"
        ));

        assertThat(response.id()).isEqualTo(15L);
        assertThat(response.priority()).isEqualTo("PRIORITY");
        assertThat(response.category()).isEqualTo("PRIORITY_SUPPORT");
        assertThat(response.slaDueAt()).isNotNull();
    }

    @Test
    void shouldCreateSsoTicketForEnterpriseRequests() {
        TenantContext.setTenantId("tenant-a");
        ReflectionTestUtils.setField(supportService, "prioritySlaHours", 4L);
        when(supportEmailService.sendAcknowledgement(any(SupportTicket.class))).thenReturn(false);
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> {
            SupportTicket ticket = invocation.getArgument(0);
            ticket.setId(21L);
            ticket.setCreatedAt(Instant.now());
            return ticket;
        });

        var response = supportService.createSsoTicket(new SupportTicketRequest(
                "Alice",
                "alice@company.com",
                "+352000000",
                "Configuration SSO",
                "Besoin d une federation OIDC"
        ));

        assertThat(response.id()).isEqualTo(21L);
        assertThat(response.category()).isEqualTo("SSO_SETUP");
        assertThat(response.priority()).isEqualTo("PRIORITY");
        assertThat(response.slaDueAt()).isNotNull();
    }

    @Test
    void shouldCreateHrNotificationWhenPlatformResolvesTicket() {
        SupportTicket ticket = new SupportTicket();
        ticket.setId(33L);
        ticket.setTenantId("tenant-a");
        ticket.setSubject("Bug paie");
        ticket.setMessage("Simulation bloquee");
        ticket.setRequesterEmail("employee@company.com");
        ticket.setCategory(SupportTicketCategory.EMAIL_SUPPORT);
        ticket.setPriority(SupportTicketPriority.STANDARD);
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setCreatedAt(Instant.now());

        when(supportTicketRepository.findById(33L)).thenReturn(Optional.of(ticket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(supportEmailService.sendResolution(any(SupportTicket.class))).thenReturn(false);

        var response = supportService.resolvePlatformTicket(33L, "Correctif applique");

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, times(2)).save(logCaptor.capture());

        assertThat(response.status()).isEqualTo("RESOLVED");
        assertThat(logCaptor.getAllValues())
                .anySatisfy(log -> {
                    assertThat(log.getTenantId()).isEqualTo("tenant-a");
                    assertThat(log.getChannel()).isEqualTo("SUPPORT_RESOLVED_HR");
                    assertThat(log.getSubject()).isEqualTo("Ticket support résolu #33");
                    assertThat(log.getPayload()).contains("Correctif applique");
                });
    }

    @Test
    void shouldResolveTenantTicketAndNotifyHr() {
        TenantContext.setTenantId("tenant-a");
        SupportTicket ticket = new SupportTicket();
        ticket.setId(44L);
        ticket.setTenantId("tenant-a");
        ticket.setSubject("Bug congés");
        ticket.setMessage("Demande bloquée");
        ticket.setRequesterEmail("employee@company.com");
        ticket.setCategory(SupportTicketCategory.EMAIL_SUPPORT);
        ticket.setPriority(SupportTicketPriority.STANDARD);
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setCreatedAt(Instant.now());

        when(supportTicketRepository.findByIdAndTenantId(44L, "tenant-a")).thenReturn(Optional.of(ticket));
        when(supportTicketRepository.save(any(SupportTicket.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(supportEmailService.sendResolution(any(SupportTicket.class))).thenReturn(false);

        var response = supportService.resolveTenantTicket(44L, "Traitement terminé");

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, times(2)).save(logCaptor.capture());

        assertThat(response.status()).isEqualTo("RESOLVED");
        assertThat(response.resolutionMessage()).isEqualTo("Traitement terminé");
        assertThat(logCaptor.getAllValues())
                .anySatisfy(log -> {
                    assertThat(log.getTenantId()).isEqualTo("tenant-a");
                    assertThat(log.getChannel()).isEqualTo("SUPPORT_RESOLVED_HR");
                    assertThat(log.getSubject()).isEqualTo("Ticket support résolu #44");
                    assertThat(log.getPayload()).contains("Traitement terminé");
                });
    }
}
