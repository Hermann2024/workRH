package com.workrh.common.subscription;

import com.workrh.common.web.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class SubscriptionFeatureClient {

    private final RestTemplate restTemplate;

    @Value("${subscription.service.base-url:http://localhost:8087}")
    private String subscriptionServiceBaseUrl;

    public SubscriptionFeatureClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public FeatureAccessResponse check(FeatureCode featureCode) {
        ServletRequestAttributes attributes = currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Tenant-Id", request.getHeader("X-Tenant-Id"));
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && !authorization.isBlank()) {
            headers.add(HttpHeaders.AUTHORIZATION, authorization);
        }

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<FeatureAccessResponse> response = restTemplate.exchange(
                subscriptionServiceBaseUrl + "/api/subscriptions/features/check?feature=" + featureCode.name(),
                HttpMethod.GET,
                entity,
                FeatureAccessResponse.class
        );
        return response.getBody();
    }

    private ServletRequestAttributes currentRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletRequestAttributes)) {
            throw new UnauthorizedException("Missing request context for subscription feature check");
        }
        return servletRequestAttributes;
    }
}
