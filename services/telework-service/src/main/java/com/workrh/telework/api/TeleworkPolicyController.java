package com.workrh.telework.api;

import com.workrh.common.subscription.FeatureCode;
import com.workrh.common.subscription.RequiresFeature;
import com.workrh.telework.api.dto.TeleworkPolicyRequest;
import com.workrh.telework.api.dto.TeleworkPolicyResponse;
import com.workrh.telework.service.TeleworkPolicyService;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/telework/policies")
@RequiresFeature(FeatureCode.TELEWORK_COMPLIANCE_34)
public class TeleworkPolicyController {

    private final TeleworkPolicyService teleworkPolicyService;

    public TeleworkPolicyController(TeleworkPolicyService teleworkPolicyService) {
        this.teleworkPolicyService = teleworkPolicyService;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public List<TeleworkPolicyResponse> list() {
        return teleworkPolicyService.list();
    }

    @GetMapping("/effective")
    @PreAuthorize("hasAnyAuthority('ADMIN','HR')")
    public TeleworkPolicyResponse effective(@RequestParam(name = "countryCode", required = false) String countryCode) {
        return teleworkPolicyService.effective(countryCode);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public TeleworkPolicyResponse create(@Valid @RequestBody TeleworkPolicyRequest request) {
        return teleworkPolicyService.create(request);
    }

    @PutMapping("/{policyId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public TeleworkPolicyResponse update(@PathVariable("policyId") Long policyId, @Valid @RequestBody TeleworkPolicyRequest request) {
        return teleworkPolicyService.update(policyId, request);
    }
}
