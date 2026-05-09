package com.workrh.telework.api.dto;

import jakarta.validation.constraints.NotBlank;

public record TeleworkComplianceEvidenceRequest(
        @NotBlank String evidenceType,
        @NotBlank String label,
        String reference,
        String fileUrl
) {
}
