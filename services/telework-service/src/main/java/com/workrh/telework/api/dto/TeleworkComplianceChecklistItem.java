package com.workrh.telework.api.dto;

public record TeleworkComplianceChecklistItem(
        String code,
        String label,
        String status,
        String severity,
        String detail,
        String sourceUrl
) {
}
