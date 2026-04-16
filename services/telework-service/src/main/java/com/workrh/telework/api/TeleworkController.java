package com.workrh.telework.api;

import com.workrh.common.subscription.FeatureCode;
import com.workrh.common.subscription.RequiresFeature;
import com.workrh.telework.api.dto.TeleworkDeclarationRequest;
import com.workrh.telework.api.dto.TeleworkDeclarationResponse;
import com.workrh.telework.api.dto.TeleworkCompanySummaryResponse;
import com.workrh.telework.api.dto.TeleworkSummaryResponse;
import com.workrh.telework.service.TeleworkService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/telework")
public class TeleworkController {

    private final TeleworkService teleworkService;

    public TeleworkController(TeleworkService teleworkService) {
        this.teleworkService = teleworkService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','HR','EMPLOYEE')")
    @RequiresFeature(FeatureCode.TELEWORK_BASIC)
    public TeleworkDeclarationResponse declare(@Valid @RequestBody TeleworkDeclarationRequest request) {
        return teleworkService.declare(request);
    }

    @GetMapping("/summary/{employeeId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR','EMPLOYEE')")
    @RequiresFeature(FeatureCode.TELEWORK_COMPLIANCE_34)
    public TeleworkSummaryResponse summary(
            @PathVariable("employeeId") Long employeeId,
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            @RequestParam(name = "countryCode", required = false) String countryCode) {
        return teleworkService.summary(employeeId, year, month, countryCode);
    }

    @GetMapping("/history/{employeeId}")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    @RequiresFeature(FeatureCode.DECLARATION_AUDIT)
    public List<TeleworkDeclarationResponse> history(@PathVariable("employeeId") Long employeeId) {
        return teleworkService.history(employeeId);
    }

    @GetMapping("/me/history")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR','EMPLOYEE')")
    @RequiresFeature(FeatureCode.TELEWORK_BASIC)
    public List<TeleworkDeclarationResponse> currentEmployeeHistory() {
        return teleworkService.currentEmployeeHistory();
    }

    @GetMapping("/company-summary")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    @RequiresFeature(FeatureCode.DASHBOARD_ADVANCED)
    public TeleworkCompanySummaryResponse companySummary(
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            @RequestParam(name = "countryCode", required = false) String countryCode) {
        return teleworkService.companySummary(year, month, countryCode);
    }
}
