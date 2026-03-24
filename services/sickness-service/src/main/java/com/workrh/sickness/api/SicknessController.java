package com.workrh.sickness.api;

import com.workrh.common.subscription.FeatureCode;
import com.workrh.common.subscription.RequiresFeature;
import com.workrh.sickness.api.dto.SicknessRequestDto;
import com.workrh.sickness.api.dto.SicknessResponseDto;
import com.workrh.sickness.service.SicknessService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sickness")
@RequiresFeature(FeatureCode.SICKNESS_MANAGEMENT)
public class SicknessController {

    private final SicknessService sicknessService;

    public SicknessController(SicknessService sicknessService) {
        this.sicknessService = sicknessService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','HR','EMPLOYEE')")
    public SicknessResponseDto declare(@Valid @RequestBody SicknessRequestDto request) {
        return sicknessService.declare(request);
    }

    @GetMapping("/{sicknessId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR','EMPLOYEE')")
    public SicknessResponseDto findById(@PathVariable Long sicknessId) {
        return sicknessService.findById(sicknessId);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public List<SicknessResponseDto> list(@RequestParam(required = false) Long employeeId) {
        return employeeId == null ? sicknessService.list() : sicknessService.listByEmployee(employeeId);
    }
}
