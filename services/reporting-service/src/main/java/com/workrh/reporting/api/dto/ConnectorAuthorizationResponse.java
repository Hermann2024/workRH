package com.workrh.reporting.api.dto;

import com.workrh.reporting.domain.HrProvider;

public record ConnectorAuthorizationResponse(
        HrProvider provider,
        String authorizationUrl,
        String state
) {
}
