package com.workrh.users.service;

import com.workrh.users.domain.Employee;
import com.workrh.users.domain.EmployeeInvitation;
import com.workrh.users.domain.TenantWorkspace;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class EmployeeInvitationNotificationClient {

    private final RestTemplate restTemplate;

    @Value("${notification.service.base-url:http://localhost:9085}")
    private String notificationServiceBaseUrl;

    @Value("${notification.internal.key:workrh-notification-internal}")
    private String internalKey;

    @Value("${workrh.public-base-url:http://localhost:4200}")
    private String publicBaseUrl;

    public EmployeeInvitationNotificationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean sendInvitation(EmployeeInvitation invitation, TenantWorkspace workspace, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Tenant-Id", invitation.getTenantId());
        headers.add("X-Internal-Key", internalKey);

        InvitationEmailRequest request = new InvitationEmailRequest(
                invitation.getEmail(),
                invitation.getFirstName(),
                invitation.getLastName(),
                workspace.getCompanyName(),
                buildInvitationUrl(token)
        );

        try {
            InvitationEmailResponse response = restTemplate.postForObject(
                    notificationServiceBaseUrl + "/api/notifications/invitations",
                    new HttpEntity<>(request, headers),
                    InvitationEmailResponse.class
            );
            return response != null && response.sent();
        } catch (RestClientException exception) {
            return false;
        }
    }

    public boolean sendPasswordReset(Employee employee, TenantWorkspace workspace, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Tenant-Id", employee.getTenantId());
        headers.add("X-Internal-Key", internalKey);

        PasswordResetEmailRequest request = new PasswordResetEmailRequest(
                employee.getEmail(),
                employee.getFirstName(),
                workspace.getCompanyName(),
                buildPasswordResetUrl(token)
        );

        try {
            InvitationEmailResponse response = restTemplate.postForObject(
                    notificationServiceBaseUrl + "/api/notifications/password-reset",
                    new HttpEntity<>(request, headers),
                    InvitationEmailResponse.class
            );
            return response != null && response.sent();
        } catch (RestClientException exception) {
            return false;
        }
    }

    private String buildInvitationUrl(String token) {
        String baseUrl = publicBaseUrl == null || publicBaseUrl.isBlank()
                ? "http://localhost:4200"
                : publicBaseUrl.trim().replaceAll("/+$", "");
        return baseUrl + "/invite?token=" + token;
    }

    private String buildPasswordResetUrl(String token) {
        String baseUrl = publicBaseUrl == null || publicBaseUrl.isBlank()
                ? "http://localhost:4200"
                : publicBaseUrl.trim().replaceAll("/+$", "");
        return baseUrl + "/reset-password?token=" + token;
    }

    private record InvitationEmailRequest(
            String email,
            String firstName,
            String lastName,
            String companyName,
            String invitationUrl
    ) {
    }

    private record InvitationEmailResponse(boolean sent) {
    }

    private record PasswordResetEmailRequest(
            String email,
            String firstName,
            String companyName,
            String resetUrl
    ) {
    }
}
