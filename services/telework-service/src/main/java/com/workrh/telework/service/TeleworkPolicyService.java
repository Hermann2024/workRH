package com.workrh.telework.service;

import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.common.web.NotFoundException;
import com.workrh.telework.api.dto.TeleworkPolicyRequest;
import com.workrh.telework.api.dto.TeleworkPolicyResponse;
import com.workrh.telework.domain.TeleworkPolicy;
import com.workrh.telework.repository.TeleworkPolicyRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TeleworkPolicyService {

    public static final String DEFAULT_COUNTRY_CODE = "DEFAULT";
    private static final int DEFAULT_ANNUAL_LIMIT = 34;
    private static final int DEFAULT_WEEKLY_LIMIT = 2;

    private final TeleworkPolicyRepository teleworkPolicyRepository;

    public TeleworkPolicyService(TeleworkPolicyRepository teleworkPolicyRepository) {
        this.teleworkPolicyRepository = teleworkPolicyRepository;
    }

    public TeleworkPolicy resolvePolicy(String countryCode) {
        String tenantId = TenantContext.getTenantId();
        String normalized = normalizeCountryCode(countryCode);
        return teleworkPolicyRepository.findByTenantIdAndCountryCodeAndActiveTrue(tenantId, normalized)
                .or(() -> teleworkPolicyRepository.findByTenantIdAndCountryCodeAndActiveTrue(tenantId, DEFAULT_COUNTRY_CODE))
                .orElseGet(this::defaultPolicy);
    }

    public TeleworkPolicyResponse create(TeleworkPolicyRequest request) {
        String tenantId = TenantContext.getTenantId();
        String countryCode = normalizeCountryCode(request.countryCode());
        if (teleworkPolicyRepository.existsByTenantIdAndCountryCode(tenantId, countryCode)) {
            throw new BadRequestException("Telework policy already exists for tenant and country");
        }
        TeleworkPolicy policy = new TeleworkPolicy();
        policy.setTenantId(tenantId);
        policy.setCountryCode(countryCode);
        policy.setAnnualFiscalLimitDays(request.annualFiscalLimitDays());
        policy.setWeeklyCompanyLimitDays(request.weeklyCompanyLimitDays());
        policy.setWeeklyLimitEnabled(request.weeklyLimitEnabled());
        policy.setActive(request.active());
        policy.setCreatedAt(Instant.now());
        policy.setUpdatedAt(Instant.now());
        return toResponse(teleworkPolicyRepository.save(policy));
    }

    public TeleworkPolicyResponse update(Long policyId, TeleworkPolicyRequest request) {
        String tenantId = TenantContext.getTenantId();
        String countryCode = normalizeCountryCode(request.countryCode());
        if (teleworkPolicyRepository.existsByTenantIdAndCountryCodeAndIdNot(tenantId, countryCode, policyId)) {
            throw new BadRequestException("Telework policy already exists for tenant and country");
        }
        TeleworkPolicy policy = teleworkPolicyRepository.findByIdAndTenantId(policyId, tenantId)
                .orElseThrow(() -> new NotFoundException("Telework policy not found"));
        policy.setCountryCode(countryCode);
        policy.setAnnualFiscalLimitDays(request.annualFiscalLimitDays());
        policy.setWeeklyCompanyLimitDays(request.weeklyCompanyLimitDays());
        policy.setWeeklyLimitEnabled(request.weeklyLimitEnabled());
        policy.setActive(request.active());
        policy.setUpdatedAt(Instant.now());
        return toResponse(teleworkPolicyRepository.save(policy));
    }

    public List<TeleworkPolicyResponse> list() {
        return teleworkPolicyRepository.findAllByTenantId(TenantContext.getTenantId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public TeleworkPolicyResponse effective(String countryCode) {
        return toResponse(resolvePolicy(countryCode));
    }

    private TeleworkPolicy defaultPolicy() {
        TeleworkPolicy policy = new TeleworkPolicy();
        policy.setCountryCode(DEFAULT_COUNTRY_CODE);
        policy.setAnnualFiscalLimitDays(DEFAULT_ANNUAL_LIMIT);
        policy.setWeeklyCompanyLimitDays(DEFAULT_WEEKLY_LIMIT);
        policy.setWeeklyLimitEnabled(true);
        policy.setActive(true);
        return policy;
    }

    private String normalizeCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return DEFAULT_COUNTRY_CODE;
        }
        return countryCode.trim().toUpperCase();
    }

    private TeleworkPolicyResponse toResponse(TeleworkPolicy policy) {
        return new TeleworkPolicyResponse(
                policy.getId(),
                policy.getCountryCode(),
                policy.getAnnualFiscalLimitDays(),
                policy.getWeeklyCompanyLimitDays(),
                policy.isWeeklyLimitEnabled(),
                policy.isActive(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}
