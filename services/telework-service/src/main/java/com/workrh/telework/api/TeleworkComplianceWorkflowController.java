package com.workrh.telework.api;

import com.workrh.common.subscription.FeatureCode;
import com.workrh.common.subscription.RequiresFeature;
import com.workrh.telework.api.dto.TeleworkComplianceCaseRequest;
import com.workrh.telework.api.dto.TeleworkComplianceCaseResponse;
import com.workrh.telework.api.dto.TeleworkComplianceEvidenceRequest;
import com.workrh.telework.api.dto.TeleworkSituationChangeRequest;
import com.workrh.telework.api.dto.TeleworkSituationChangeResponse;
import com.workrh.telework.service.TeleworkComplianceWorkflowService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telework/compliance")
@PreAuthorize("hasAnyAuthority('ADMIN','HR')")
@RequiresFeature(FeatureCode.DECLARATION_AUDIT)
public class TeleworkComplianceWorkflowController {

    private final TeleworkComplianceWorkflowService workflowService;

    public TeleworkComplianceWorkflowController(TeleworkComplianceWorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @GetMapping("/cases")
    public List<TeleworkComplianceCaseResponse> cases(@RequestParam("year") int year, @RequestParam("month") int month) {
        return workflowService.list(year, month);
    }

    @PostMapping("/cases")
    public TeleworkComplianceCaseResponse createCase(@Valid @RequestBody TeleworkComplianceCaseRequest request) {
        return workflowService.createOrUpdate(request);
    }

    @PutMapping("/cases/{caseId}")
    public TeleworkComplianceCaseResponse updateCase(
            @PathVariable("caseId") Long caseId,
            @Valid @RequestBody TeleworkComplianceCaseRequest request) {
        return workflowService.update(caseId, request);
    }

    @PostMapping("/cases/{caseId}/evidence")
    public TeleworkComplianceCaseResponse addEvidence(
            @PathVariable("caseId") Long caseId,
            @Valid @RequestBody TeleworkComplianceEvidenceRequest request) {
        return workflowService.addEvidence(caseId, request);
    }

    @PostMapping("/cases/{caseId}/validate")
    public TeleworkComplianceCaseResponse validate(@PathVariable("caseId") Long caseId) {
        return workflowService.validateForPayroll(caseId);
    }

    @PostMapping("/situation-changes")
    public TeleworkSituationChangeResponse recordSituationChange(@Valid @RequestBody TeleworkSituationChangeRequest request) {
        return workflowService.recordSituationChange(request);
    }

    @GetMapping("/cases/export.csv")
    public ResponseEntity<byte[]> exportCases(@RequestParam("year") int year, @RequestParam("month") int month) {
        byte[] csv = workflowService.exportCasesCsv(year, month);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("workrh-telework-compliance-" + year + "-" + String.format("%02d", month) + ".csv")
                        .build()
                        .toString())
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }
}
