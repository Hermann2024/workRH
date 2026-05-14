package com.workrh.subscription.service;

import com.workrh.common.tenant.TenantContext;
import com.workrh.subscription.domain.SubscriptionStatus;
import com.workrh.subscription.domain.TenantSubscription;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class WorkspaceSubscriptionSyncClient {

    private final RestTemplate restTemplate;

    @Value("${subscription.user-service.base-url:http://localhost:9081}")
    private String userServiceBaseUrl;

    @Value("${subscription.workspace.internal-key:workrh-workspace-internal}")
    private String internalKey;

    public WorkspaceSubscriptionSyncClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sync(TenantSubscription subscription) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Tenant-Id", subscription.getTenantId() != null ? subscription.getTenantId() : TenantContext.getTenantId());
        headers.add("X-Internal-Key", internalKey);

        WorkspaceSubscriptionSyncRequest request = new WorkspaceSubscriptionSyncRequest(
                subscription.getPlanCode().name(),
                subscription.getSeatsPurchased(),
                subscription.getStripeCustomerEmail(),
                subscription.getStatus() != SubscriptionStatus.CANCELLED
        );

        try {
            restTemplate.postForEntity(
                    userServiceBaseUrl + "/api/internal/workspaces/subscription",
                    new HttpEntity<>(request, headers),
                    Void.class
            );
        } catch (RestClientException exception) {
            throw new IllegalStateException("Unable to synchronize workspace subscription.", exception);
        }
    }

    private record WorkspaceSubscriptionSyncRequest(
            String planCode,
            int seatsPurchased,
            String ownerEmail,
            boolean active
    ) {
    }
}
