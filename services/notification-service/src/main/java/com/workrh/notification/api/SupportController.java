package com.workrh.notification.api;

import com.workrh.common.subscription.FeatureCode;
import com.workrh.common.subscription.RequiresFeature;
import com.workrh.notification.api.dto.SlaTicketResponse;
import com.workrh.notification.api.dto.SupportTicketRequest;
import com.workrh.notification.api.dto.SupportTicketResponse;
import com.workrh.notification.api.dto.SupportTicketResolutionRequest;
import com.workrh.notification.service.SupportService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/support")
@PreAuthorize("hasAnyAuthority('ADMIN','HR','EMPLOYEE','PLATFORM_ADMIN')")
public class SupportController {

    private final SupportService supportService;

    public SupportController(SupportService supportService) {
        this.supportService = supportService;
    }

    @GetMapping("/tickets")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    @RequiresFeature(FeatureCode.EMAIL_SUPPORT)
    public List<SupportTicketResponse> listTickets() {
        return supportService.listTickets();
    }

    @GetMapping("/platform/tickets")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public List<SupportTicketResponse> listPlatformTickets() {
        return supportService.listPlatformTickets();
    }

    @PostMapping("/platform/tickets/{ticketId}/resolve")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public SupportTicketResponse resolvePlatformTicket(
            @PathVariable Long ticketId,
            @RequestBody SupportTicketResolutionRequest request) {
        return supportService.resolvePlatformTicket(ticketId, request.message());
    }

    @PostMapping("/tickets/{ticketId}/resolve")
    @PreAuthorize("hasAuthority('ADMIN')")
    public SupportTicketResponse resolveTenantTicket(
            @PathVariable Long ticketId,
            @RequestBody SupportTicketResolutionRequest request) {
        return supportService.resolveTenantTicket(ticketId, request.message());
    }

    @DeleteMapping("/platform/tickets/{ticketId}")
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    public void deletePlatformTicket(@PathVariable Long ticketId) {
        supportService.deletePlatformTicket(ticketId);
    }

    /**
     * Standard tickets are not gated on {@link FeatureCode#EMAIL_SUPPORT}: tenants must be able to
     * report outages or billing issues even when a trial expired, subscription rows are missing, or
     * the plan flag is temporarily out of sync. Premium channels remain feature-gated below.
     */
    @PostMapping("/tickets")
    @PreAuthorize("hasAnyAuthority('HR','EMPLOYEE')")
    public SupportTicketResponse createTicket(@RequestBody SupportTicketRequest request) {
        return supportService.createStandardTicket(request);
    }

    @PostMapping("/tickets/priority")
    @PreAuthorize("hasAnyAuthority('HR','EMPLOYEE')")
    @RequiresFeature(FeatureCode.PRIORITY_SUPPORT)
    public SupportTicketResponse createPriorityTicket(@RequestBody SupportTicketRequest request) {
        return supportService.createPriorityTicket(request);
    }

    @PostMapping("/tickets/onboarding")
    @PreAuthorize("hasAnyAuthority('HR','EMPLOYEE')")
    @RequiresFeature(FeatureCode.ONBOARDING_SUPPORT)
    public SupportTicketResponse createOnboardingTicket(@RequestBody SupportTicketRequest request) {
        return supportService.createOnboardingTicket(request);
    }

    @PostMapping("/tickets/sso")
    @PreAuthorize("hasAnyAuthority('HR','EMPLOYEE')")
    @RequiresFeature(FeatureCode.SSO)
    public SupportTicketResponse createSsoTicket(@RequestBody SupportTicketRequest request) {
        return supportService.createSsoTicket(request);
    }

    @PostMapping("/tickets/security")
    @PreAuthorize("hasAnyAuthority('HR','EMPLOYEE')")
    @RequiresFeature(FeatureCode.HARDENED_SECURITY)
    public SupportTicketResponse createSecurityTicket(@RequestBody SupportTicketRequest request) {
        return supportService.createSecurityTicket(request);
    }

    @PostMapping("/tickets/hosting")
    @PreAuthorize("hasAnyAuthority('HR','EMPLOYEE')")
    @RequiresFeature(FeatureCode.DEDICATED_HOSTING)
    public SupportTicketResponse createHostingTicket(@RequestBody SupportTicketRequest request) {
        return supportService.createHostingTicket(request);
    }

    @PostMapping("/tickets/custom-development")
    @PreAuthorize("hasAnyAuthority('HR','EMPLOYEE')")
    @RequiresFeature(FeatureCode.CUSTOM_DEVELOPMENT)
    public SupportTicketResponse createCustomDevelopmentTicket(@RequestBody SupportTicketRequest request) {
        return supportService.createCustomDevelopmentTicket(request);
    }

    @PostMapping("/tickets/integration")
    @PreAuthorize("hasAnyAuthority('HR','EMPLOYEE')")
    @RequiresFeature(FeatureCode.PUBLIC_API)
    public SupportTicketResponse createIntegrationTicket(@RequestBody SupportTicketRequest request) {
        return supportService.createIntegrationTicket(request);
    }

    @GetMapping("/tickets/sla")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    @RequiresFeature(FeatureCode.SLA_SUPPORT)
    public List<SlaTicketResponse> listSlaTickets() {
        return supportService.listSlaTickets();
    }
}
