package com.workrh.notification.api;

import com.workrh.common.subscription.FeatureCode;
import com.workrh.common.subscription.RequiresFeature;
import com.workrh.notification.api.dto.SlaTicketResponse;
import com.workrh.notification.api.dto.SupportTicketRequest;
import com.workrh.notification.api.dto.SupportTicketResponse;
import com.workrh.notification.service.SupportService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support")
@PreAuthorize("hasAnyAuthority('ADMIN','HR')")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping("/tickets")
    @RequiresFeature(FeatureCode.EMAIL_SUPPORT)
    public List<SupportTicketResponse> listTickets() {
        return supportService.listTickets();
    }

    @PostMapping("/tickets")
    @RequiresFeature(FeatureCode.EMAIL_SUPPORT)
    public SupportTicketResponse createTicket(@RequestBody SupportTicketRequest request) {
        return supportService.createStandardTicket(request);
    }

    @PostMapping("/tickets/priority")
    @RequiresFeature(FeatureCode.PRIORITY_SUPPORT)
    public SupportTicketResponse createPriorityTicket(@RequestBody SupportTicketRequest request) {
        return supportService.createPriorityTicket(request);
    }

    @PostMapping("/tickets/onboarding")
    @RequiresFeature(FeatureCode.ONBOARDING_SUPPORT)
    public SupportTicketResponse createOnboardingTicket(@RequestBody SupportTicketRequest request) {
        return supportService.createOnboardingTicket(request);
    }

    @PostMapping("/tickets/sso")
    @RequiresFeature(FeatureCode.SSO)
    public SupportTicketResponse createSsoTicket(@RequestBody SupportTicketRequest request) {
        return supportService.createSsoTicket(request);
    }

    @PostMapping("/tickets/security")
    @RequiresFeature(FeatureCode.HARDENED_SECURITY)
    public SupportTicketResponse createSecurityTicket(@RequestBody SupportTicketRequest request) {
        return supportService.createSecurityTicket(request);
    }

    @PostMapping("/tickets/hosting")
    @RequiresFeature(FeatureCode.DEDICATED_HOSTING)
    public SupportTicketResponse createHostingTicket(@RequestBody SupportTicketRequest request) {
        return supportService.createHostingTicket(request);
    }

    @PostMapping("/tickets/custom-development")
    @RequiresFeature(FeatureCode.CUSTOM_DEVELOPMENT)
    public SupportTicketResponse createCustomDevelopmentTicket(@RequestBody SupportTicketRequest request) {
        return supportService.createCustomDevelopmentTicket(request);
    }

    @PostMapping("/tickets/integration")
    @RequiresFeature(FeatureCode.PUBLIC_API)
    public SupportTicketResponse createIntegrationTicket(@RequestBody SupportTicketRequest request) {
        return supportService.createIntegrationTicket(request);
    }

    @GetMapping("/tickets/sla")
    @RequiresFeature(FeatureCode.SLA_SUPPORT)
    public List<SlaTicketResponse> listSlaTickets() {
        return supportService.listSlaTickets();
    }
}
