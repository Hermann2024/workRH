package com.workrh.telework.api.dto;

import java.time.Instant;

public record TeleworkComplianceEvidenceResponse(
        Long id,
        String evidenceType,
        String label,
        String reference,
        String fileUrl,
        String uploadedBy,
        Instant uploadedAt
) {
}
