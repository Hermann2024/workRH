package com.workrh.sickness.api;

import com.workrh.common.subscription.FeatureCode;
import com.workrh.common.subscription.RequiresFeature;
import com.workrh.sickness.api.dto.SicknessRequestDto;
import com.workrh.sickness.api.dto.SicknessResponseDto;
import com.workrh.sickness.service.SicknessService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/sickness")
@RequiresFeature(FeatureCode.SICKNESS_MANAGEMENT)
public class SicknessController {

    private final SicknessService sicknessService;

    public SicknessController(SicknessService sicknessService) {
        this.sicknessService = sicknessService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('HR','EMPLOYEE')")
    public SicknessResponseDto declare(@Valid @RequestBody SicknessRequestDto request) {
        return sicknessService.declare(request);
    }

    @GetMapping("/{sicknessId}")
    @PreAuthorize("hasAnyAuthority('HR','EMPLOYEE')")
    public SicknessResponseDto findById(@PathVariable("sicknessId") Long sicknessId) {
        return sicknessService.findById(sicknessId);
    }

    @PostMapping(value = "/{sicknessId}/evidence", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('HR','EMPLOYEE')")
    public SicknessResponseDto uploadEvidence(
            @PathVariable("sicknessId") Long sicknessId,
            @RequestParam("file") MultipartFile file) {
        return sicknessService.uploadEvidence(sicknessId, file);
    }

    @GetMapping("/{sicknessId}/evidence")
    @PreAuthorize("hasAnyAuthority('HR','EMPLOYEE')")
    public ResponseEntity<byte[]> downloadEvidence(@PathVariable("sicknessId") Long sicknessId) {
        SicknessService.EvidenceDownload evidence = sicknessService.downloadEvidence(sicknessId);
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(evidence.fileName()).build().toString()
                )
                .contentType(MediaType.parseMediaType(evidence.contentType()))
                .body(evidence.content());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('HR')")
    public List<SicknessResponseDto> list(@RequestParam(name = "employeeId", required = false) Long employeeId) {
        return employeeId == null ? sicknessService.list() : sicknessService.listByEmployee(employeeId);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyAuthority('HR','EMPLOYEE')")
    public List<SicknessResponseDto> listCurrentEmployee() {
        return sicknessService.listCurrentEmployee();
    }
}
