package com.workrh.gateway;

import java.util.Arrays;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class GatewayProductionReadinessGuard implements ApplicationRunner {

    private final Environment environment;

    public GatewayProductionReadinessGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean explicit = environment.getProperty("workrh.production-readiness.enforced", Boolean.class, false);
        boolean prodProfile = Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
        if (!explicit && !prodProfile) {
            return;
        }

        String frontendOrigin = environment.getProperty("workrh.frontend-origin",
                environment.getProperty("WORKRH_FRONTEND_ORIGIN", ""));
        if (frontendOrigin == null
                || frontendOrigin.isBlank()
                || "*".equals(frontendOrigin.trim())
                || frontendOrigin.contains("localhost")
                || frontendOrigin.contains("127.0.0.1")) {
            throw new IllegalStateException("WORKRH_FRONTEND_ORIGIN must be a public HTTPS origin in production");
        }
        if (!frontendOrigin.startsWith("https://")) {
            throw new IllegalStateException("WORKRH_FRONTEND_ORIGIN must use HTTPS in production");
        }
    }
}
