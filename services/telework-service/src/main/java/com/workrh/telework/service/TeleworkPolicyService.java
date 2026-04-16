package com.workrh.telework.service;

import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.BadRequestException;
import com.workrh.common.web.NotFoundException;
import com.workrh.telework.api.dto.TeleworkPolicyRequest;
import com.workrh.telework.api.dto.TeleworkPolicyResponse;
import com.workrh.telework.domain.TeleworkPolicy;
import com.workrh.telework.repository.TeleworkPolicyRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TeleworkPolicyService {

    public static final String DEFAULT_COUNTRY_CODE = "DEFAULT";
    private static final int DEFAULT_ANNUAL_LIMIT = 34;
    private static final int DEFAULT_WEEKLY_LIMIT = 2;
    private static final int DEFAULT_STANDARD_DAILY_WORK_MINUTES = 480;
    private static final int SOCIAL_SECURITY_STANDARD_THRESHOLD = 25;
    private static final int SOCIAL_SECURITY_FRAMEWORK_THRESHOLD = 49;
    private static final List<String> BUILT_IN_COUNTRY_CODES = List.of("FR", "BE", "DE", DEFAULT_COUNTRY_CODE);

    private final TeleworkPolicyRepository teleworkPolicyRepository;

    public TeleworkPolicyService(TeleworkPolicyRepository teleworkPolicyRepository) {
        this.teleworkPolicyRepository = teleworkPolicyRepository;
    }

    public TeleworkPolicy resolvePolicy(String countryCode) {
        String tenantId = TenantContext.getTenantId();
        String normalized = normalizeCountryCode(countryCode);
        return teleworkPolicyRepository.findByTenantIdAndCountryCodeAndActiveTrue(tenantId, normalized)
                .or(() -> builtInLuxembourgFrontierPolicy(normalized))
                .or(() -> teleworkPolicyRepository.findByTenantIdAndCountryCodeAndActiveTrue(tenantId, DEFAULT_COUNTRY_CODE))
                .orElseGet(() -> defaultPolicy(DEFAULT_COUNTRY_CODE));
    }

    public TeleworkPolicyResponse create(TeleworkPolicyRequest request) {
        String tenantId = TenantContext.getTenantId();
        String countryCode = normalizeCountryCode(request.countryCode());
        if (teleworkPolicyRepository.existsByTenantIdAndCountryCode(tenantId, countryCode)) {
            throw new BadRequestException("Telework policy already exists for tenant and country");
        }

        TeleworkPolicy policy = new TeleworkPolicy();
        policy.setTenantId(tenantId);
        applyRequest(policy, request, countryCode);
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
        applyRequest(policy, request, countryCode);
        policy.setUpdatedAt(Instant.now());
        return toResponse(teleworkPolicyRepository.save(policy));
    }

    public List<TeleworkPolicyResponse> list() {
        Map<String, TeleworkPolicy> persistedPolicies = teleworkPolicyRepository.findAllByTenantId(TenantContext.getTenantId()).stream()
                .collect(Collectors.toMap(TeleworkPolicy::getCountryCode, policy -> policy, (left, right) -> right));

        List<TeleworkPolicyResponse> responses = new ArrayList<>();
        for (String countryCode : BUILT_IN_COUNTRY_CODES) {
            responses.add(toResponse(persistedPolicies.getOrDefault(countryCode, defaultPolicy(countryCode))));
        }

        persistedPolicies.entrySet().stream()
                .filter(entry -> !BUILT_IN_COUNTRY_CODES.contains(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .map(this::toResponse)
                .forEach(responses::add);
        return responses;
    }

    public TeleworkPolicyResponse effective(String countryCode) {
        return toResponse(resolvePolicy(countryCode));
    }

    private void applyRequest(TeleworkPolicy policy, TeleworkPolicyRequest request, String countryCode) {
        policy.setCountryCode(countryCode);
        policy.setAnnualFiscalLimitDays(request.annualFiscalLimitDays());
        policy.setWeeklyCompanyLimitDays(request.weeklyCompanyLimitDays());
        policy.setStandardDailyWorkMinutes(defaultStandardDailyWorkMinutes(request.standardDailyWorkMinutes()));
        policy.setWeeklyLimitEnabled(request.weeklyLimitEnabled());
        policy.setSocialSecurityStandardThresholdPercent(request.socialSecurityStandardThresholdPercent());
        policy.setSocialSecurityFrameworkThresholdPercent(request.socialSecurityFrameworkThresholdPercent());
        policy.setShortActivityToleranceMinutes(request.shortActivityToleranceMinutes());
        policy.setSocialSecurityFrameworkAgreementEligible(request.socialSecurityFrameworkAgreementEligible());
        policy.setPartialDayCountsAsFullDay(request.partialDayCountsAsFullDay());
        policy.setThresholdProrated(request.thresholdProrated());
        policy.setThirdCountryDaysCounted(request.thirdCountryDaysCounted());
        policy.setTaxRuleLabel(request.taxRuleLabel());
        policy.setLegalReference(request.legalReference());
        policy.setTaxSourceUrl(request.taxSourceUrl());
        policy.setSocialSecuritySourceUrl(request.socialSecuritySourceUrl());
        policy.setNotes(request.notes());
        policy.setActive(request.active());
    }

    private Optional<TeleworkPolicy> builtInLuxembourgFrontierPolicy(String countryCode) {
        return switch (countryCode) {
            case "FR" -> Optional.of(defaultPolicy("FR"));
            case "BE" -> Optional.of(defaultPolicy("BE"));
            case "DE" -> Optional.of(defaultPolicy("DE"));
            default -> Optional.empty();
        };
    }

    private TeleworkPolicy defaultPolicy(String countryCode) {
        TeleworkPolicy policy = new TeleworkPolicy();
        String normalized = normalizeCountryCode(countryCode);
        policy.setCountryCode(normalized);
        policy.setAnnualFiscalLimitDays(DEFAULT_ANNUAL_LIMIT);
        policy.setWeeklyCompanyLimitDays(DEFAULT_WEEKLY_LIMIT);
        policy.setStandardDailyWorkMinutes(DEFAULT_STANDARD_DAILY_WORK_MINUTES);
        policy.setWeeklyLimitEnabled(true);
        policy.setSocialSecurityStandardThresholdPercent(SOCIAL_SECURITY_STANDARD_THRESHOLD);
        policy.setSocialSecurityFrameworkThresholdPercent(SOCIAL_SECURITY_FRAMEWORK_THRESHOLD);
        policy.setSocialSecurityFrameworkAgreementEligible(!DEFAULT_COUNTRY_CODE.equals(normalized));
        policy.setShortActivityToleranceMinutes("DE".equals(normalized) ? 30 : 0);
        policy.setPartialDayCountsAsFullDay(true);
        policy.setThresholdProrated(false);
        policy.setThirdCountryDaysCounted(true);
        policy.setTaxSourceUrl("https://impotsdirects.public.lu/fr/az/t/teletravail.html");
        policy.setSocialSecuritySourceUrl("https://ccss.public.lu/fr/employeurs/secteur-prive/activite-etranger/accord-teletravail.html");
        applyBuiltInMetadata(policy, normalized);
        policy.setActive(true);
        return policy;
    }

    private void applyBuiltInMetadata(TeleworkPolicy policy, String countryCode) {
        switch (countryCode) {
            case "FR" -> {
                policy.setTaxRuleLabel("Luxembourg-France : tolerance fiscale de 34 jours");
                policy.setLegalReference("FAQ ACD non-residents ; accord-cadre CCSS teletravail");
                policy.setNotes("Les jours de teletravail, de deplacement professionnel et de formation hors Luxembourg comptent dans le suivi fiscal annuel.");
            }
            case "BE" -> {
                policy.setTaxRuleLabel("Luxembourg-Belgique : tolerance fiscale de 34 jours");
                policy.setLegalReference("FAQ ACD non-residents ; accord-cadre CCSS teletravail");
                policy.setNotes("Les jours de teletravail, de deplacement professionnel et de formation hors Luxembourg comptent dans le suivi fiscal annuel.");
            }
            case "DE" -> {
                policy.setTaxRuleLabel("Luxembourg-Allemagne : tolerance fiscale de 34 jours");
                policy.setLegalReference("FAQ ACD non-residents ; accord-cadre CCSS teletravail");
                policy.setNotes("Les jours partiels hors Luxembourg comptent en principe pour le seuil fiscal, avec une tolerance technique de 30 minutes configuree pour l'Allemagne.");
            }
            default -> {
                policy.setTaxRuleLabel("Politique Luxembourg frontaliers par defaut");
                policy.setLegalReference("Guichet.lu teletravail ; FAQ ACD ; CCSS accord-cadre teletravail");
                policy.setNotes("Politique generique de repli. Pour les frontaliers, privilegier FR, BE ou DE pour appliquer les seuils officiels.");
            }
        }
    }

    private int defaultStandardDailyWorkMinutes(Integer standardDailyWorkMinutes) {
        if (standardDailyWorkMinutes == null || standardDailyWorkMinutes < 60) {
            return DEFAULT_STANDARD_DAILY_WORK_MINUTES;
        }
        return standardDailyWorkMinutes;
    }

    private String normalizeCountryCode(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return DEFAULT_COUNTRY_CODE;
        }
        return countryCode.trim().toUpperCase(Locale.ROOT);
    }

    private TeleworkPolicyResponse toResponse(TeleworkPolicy policy) {
        return new TeleworkPolicyResponse(
                policy.getId(),
                policy.getCountryCode(),
                policy.getAnnualFiscalLimitDays(),
                policy.getWeeklyCompanyLimitDays(),
                policy.getStandardDailyWorkMinutes(),
                policy.getSocialSecurityStandardThresholdPercent(),
                policy.getSocialSecurityFrameworkThresholdPercent(),
                policy.getShortActivityToleranceMinutes(),
                policy.isWeeklyLimitEnabled(),
                policy.isSocialSecurityFrameworkAgreementEligible(),
                policy.isPartialDayCountsAsFullDay(),
                policy.isThresholdProrated(),
                policy.isThirdCountryDaysCounted(),
                policy.getTaxRuleLabel(),
                policy.getLegalReference(),
                policy.getTaxSourceUrl(),
                policy.getSocialSecuritySourceUrl(),
                policy.getNotes(),
                policy.isActive(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}
