package com.workrh.reporting.api.dto;

import com.workrh.reporting.domain.ConnectorAuthType;
import com.workrh.reporting.domain.ConnectorSyncMode;
import com.workrh.reporting.domain.HrProvider;
import java.util.List;
import java.util.Map;

public record ConnectorCatalogItem(
        HrProvider provider,
        String displayName,
        List<ConnectorAuthType> authTypes,
        List<ConnectorSyncMode> syncModes,
        Map<String, String> defaultFieldMappings,
        Map<String, String> defaultApiSettings
) {
}
