package com.workrh.users.service;

import com.workrh.common.web.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class SubscriptionBootstrapClient {

    private final RestTemplate restTemplate;

    @Value("${subscription.service.base-url:http://localhost:9087}")
    private String subscriptionServiceBaseUrl;

    @Value("${subscription.bootstrap.key:workrh-signup-bootstrap}")
    private String bootstrapKey;

    public SubscriptionBootstrapClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void initializeTrial(String tenantId, String ownerEmail, int seatsPurchased, String planCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Tenant-Id", tenantId);
        headers.add("X-Bootstrap-Key", bootstrapKey);

        HttpEntity<SubscriptionBootstrapRequest> entity = new HttpEntity<>(
                new SubscriptionBootstrapRequest(ownerEmail, seatsPurchased, planCode),
                headers
        );

        try {
            restTemplate.postForEntity(subscriptionServiceBaseUrl + "/api/subscriptions/bootstrap", entity, Void.class);
        } catch (HttpStatusCodeException exception) {
            throw new BadRequestException("Unable to initialize the trial for this workspace.");
        } catch (RestClientException exception) {
            throw new IllegalStateException("Subscription service is unavailable during signup.", exception);
        }
    }

    private record SubscriptionBootstrapRequest(
            String ownerEmail,
            int seatsPurchased,
            String planCode
    ) {
    }
}
