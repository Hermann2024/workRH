package com.workrh.common.subscription;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SubscriptionClientConfig {

    @Bean
    RestTemplate subscriptionRestTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}
