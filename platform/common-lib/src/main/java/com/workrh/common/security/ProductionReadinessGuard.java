package com.workrh.common.security;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionReadinessGuard implements ApplicationRunner {

    private static final List<String> UNSAFE_DDL_MODES = List.of("create", "create-drop", "update");
    private static final List<String> DEFAULT_INTERNAL_KEYS = List.of(
            "workrh-notification-internal",
            "workrh-workspace-internal",
            "workrh-subscription-bootstrap"
    );

    private final Environment environment;

    public ProductionReadinessGuard(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!isProductionGuardEnabled()) {
            return;
        }

        List<String> failures = new ArrayList<>();
        requireFalse("security.demo-authentication-enabled", failures);
        requireJwtSecret(failures);
        rejectUnsafeDdl(failures);
        rejectDefaultSecret("notification.internal.key", failures);
        rejectDefaultSecret("workspace.internal.key", failures);
        rejectDefaultSecret("subscription.bootstrap.key", failures);
        requireServiceSpecificConfiguration(failures);

        if (!failures.isEmpty()) {
            throw new IllegalStateException("WorkRH production readiness check failed: " + String.join("; ", failures));
        }
    }

    private boolean isProductionGuardEnabled() {
        boolean explicit = environment.getProperty("workrh.production-readiness.enforced", Boolean.class, false);
        boolean prodProfile = Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
        return explicit || prodProfile;
    }

    private void requireFalse(String property, List<String> failures) {
        if (environment.getProperty(property, Boolean.class, false)) {
            failures.add(property + " must be false in production");
        }
    }

    private void requireJwtSecret(List<String> failures) {
        String secret = environment.getProperty("security.jwt.secret");
        if (isBlank(secret)) {
            failures.add("security.jwt.secret is required");
            return;
        }
        try {
            byte[] decoded = Decoders.BASE64.decode(secret);
            if (decoded.length < 32) {
                failures.add("security.jwt.secret must decode to at least 32 bytes");
            }
        } catch (DecodingException | IllegalArgumentException exception) {
            failures.add("security.jwt.secret must be valid Base64");
        }
    }

    private void rejectUnsafeDdl(List<String> failures) {
        String ddlMode = environment.getProperty("spring.jpa.hibernate.ddl-auto", "validate").trim().toLowerCase();
        if (UNSAFE_DDL_MODES.contains(ddlMode)) {
            failures.add("spring.jpa.hibernate.ddl-auto=" + ddlMode + " is not allowed in production");
        }
    }

    private void rejectDefaultSecret(String property, List<String> failures) {
        String value = environment.getProperty(property);
        if (value == null) {
            return;
        }
        if (isBlank(value)) {
            failures.add(property + " is required");
            return;
        }
        if (DEFAULT_INTERNAL_KEYS.contains(value.trim())) {
            failures.add(property + " uses an unsafe default value");
        }
    }

    private void requireServiceSpecificConfiguration(List<String> failures) {
        String app = environment.getProperty("spring.application.name", "");
        if ("subscription-service".equals(app)) {
            requirePresent("stripe.secret-key", failures);
            requirePresent("stripe.webhook.secret", failures);
            requirePresent("stripe.prices.starter", failures);
            requirePresent("stripe.prices.pro", failures);
            requirePresent("stripe.prices.premium", failures);
            requirePresent("workspace.internal.key", failures);
        }
        if ("notification-service".equals(app)) {
            requirePresent("notification.internal.key", failures);
            requirePresent("spring.mail.host", failures);
            requirePresent("spring.mail.username", failures);
            requirePresent("spring.mail.password", failures);
        }
        if ("user-service".equals(app)) {
            requirePresent("workspace.internal.key", failures);
            requirePresent("subscription.bootstrap.key", failures);
        }
    }

    private void requirePresent(String property, List<String> failures) {
        if (isBlank(environment.getProperty(property))) {
            failures.add(property + " is required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
