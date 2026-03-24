package com.workrh.common.subscription;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RequiresFeatureAspect {

    private final SubscriptionFeatureClient subscriptionFeatureClient;

    public RequiresFeatureAspect(SubscriptionFeatureClient subscriptionFeatureClient) {
        this.subscriptionFeatureClient = subscriptionFeatureClient;
    }

    @Around("@within(requiresFeature) || @annotation(requiresFeature)")
    public Object enforceFeature(ProceedingJoinPoint joinPoint, RequiresFeature requiresFeature) throws Throwable {
        if (requiresFeature == null) {
            return joinPoint.proceed();
        }
        FeatureAccessResponse response = subscriptionFeatureClient.check(requiresFeature.value());
        if (response == null || !response.allowed()) {
            throw new AccessDeniedException(response != null ? response.reason() : "Subscription feature denied");
        }
        return joinPoint.proceed();
    }
}
