package com.workrh.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.workrh.common.tenant.TenantContext;
import com.workrh.notification.api.dto.SupportTicketRequest;
import com.workrh.notification.domain.SupportTicket;
import com.workrh.notification.repository.NotificationLogRepository;
import com.workrh.notification.repository.SupportTicketRepository;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
}
