package com.workrh.reporting.api.dto;

import com.workrh.reporting.domain.HrProvider;
import java.time.Instant;

public record ConnectorSyncResponse(
        Long syncRunId,
        HrProvider provider,
        String status,
        String message,
        int importedRecords,
        int skippedRecords,
        Instant startedAt,
        Instant finishedAt
) {
}
