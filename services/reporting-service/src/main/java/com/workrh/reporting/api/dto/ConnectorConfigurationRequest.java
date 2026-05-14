package com.workrh.reporting.api.dto;

import com.workrh.reporting.domain.ConnectorAuthType;
import com.workrh.reporting.domain.ConnectorSyncMode;
import java.time.Instant;
import java.util.Map;

public record ConnectorConfigurationRequest(
        boolean enabled,
        ConnectorAuthType authType,
        ConnectorSyncMode syncMode,
        String clientId,
        String clientSecret,
        String apiBaseUrl,
        String authorizationUrl,
        String tokenUrl,
        String redirectUri,
        String scopes,
        String accessToken,
        String refreshToken,
        Instant tokenExpiresAt,
        Map<String, String> fieldMappings,
        Map<String, String> apiSettings
) {
}
