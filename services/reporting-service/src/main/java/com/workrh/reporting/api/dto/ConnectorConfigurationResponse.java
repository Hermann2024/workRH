package com.workrh.reporting.api.dto;

import com.workrh.reporting.domain.ConnectorAuthType;
import com.workrh.reporting.domain.ConnectorStatus;
import com.workrh.reporting.domain.ConnectorSyncMode;
import com.workrh.reporting.domain.HrProvider;
import java.time.Instant;
import java.util.Map;

public record ConnectorConfigurationResponse(
        Long id,
        HrProvider provider,
        String displayName,
        boolean enabled,
        ConnectorAuthType authType,
        ConnectorSyncMode syncMode,
        ConnectorStatus status,
        String apiBaseUrl,
        String authorizationUrl,
        String tokenUrl,
        String redirectUri,
        String scopes,
        boolean hasClientSecret,
        boolean hasAccessToken,
        boolean hasRefreshToken,
        Instant tokenExpiresAt,
        Instant lastSyncAt,
        String lastSyncStatus,
        String lastSyncMessage,
        Map<String, String> fieldMappings,
        Map<String, String> apiSettings
) {
}
