package com.workrh.reporting.service;

import com.workrh.common.tenant.TenantContext;
import com.workrh.common.web.NotFoundException;
import com.workrh.reporting.api.dto.ConnectorAuthorizationResponse;
import com.workrh.reporting.api.dto.ConnectorCatalogItem;
import com.workrh.reporting.api.dto.ConnectorConfigurationRequest;
import com.workrh.reporting.api.dto.ConnectorConfigurationResponse;
import com.workrh.reporting.api.dto.ConnectorSyncResponse;
import com.workrh.reporting.domain.ConnectorAuthType;
import com.workrh.reporting.domain.ConnectorStatus;
import com.workrh.reporting.domain.ConnectorSyncMode;
import com.workrh.reporting.domain.HrConnectorConfiguration;
import com.workrh.reporting.domain.HrConnectorSyncRun;
import com.workrh.reporting.domain.HrProvider;
import com.workrh.reporting.repository.HrConnectorConfigurationRepository;
import com.workrh.reporting.repository.HrConnectorSyncRunRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HrConnectorService {

    private static final Map<HrProvider, Map<String, String>> DEFAULT_FIELD_MAPPINGS = defaultMappings();
    private static final Map<HrProvider, Map<String, String>> DEFAULT_API_SETTINGS = defaultApiSettings();

    private final HrConnectorConfigurationRepository configurationRepository;
    private final HrConnectorSyncRunRepository syncRunRepository;
    private final WorkdayConnectorClient workdayConnectorClient;
    private final FactorialConnectorClient factorialConnectorClient;
    private final BambooHrConnectorClient bambooHrConnectorClient;
    private final LuccaConnectorClient luccaConnectorClient;
    private final SapConnectorClient sapConnectorClient;

    public HrConnectorService(
            HrConnectorConfigurationRepository configurationRepository,
            HrConnectorSyncRunRepository syncRunRepository,
            WorkdayConnectorClient workdayConnectorClient,
            FactorialConnectorClient factorialConnectorClient,
            BambooHrConnectorClient bambooHrConnectorClient,
            LuccaConnectorClient luccaConnectorClient,
            SapConnectorClient sapConnectorClient
    ) {
        this.configurationRepository = configurationRepository;
        this.syncRunRepository = syncRunRepository;
        this.workdayConnectorClient = workdayConnectorClient;
        this.factorialConnectorClient = factorialConnectorClient;
        this.bambooHrConnectorClient = bambooHrConnectorClient;
        this.luccaConnectorClient = luccaConnectorClient;
        this.sapConnectorClient = sapConnectorClient;
    }

    public List<ConnectorCatalogItem> catalog() {
        return Arrays.stream(HrProvider.values())
                .map(provider -> new ConnectorCatalogItem(
                        provider,
                        provider.displayName(),
                        List.of(ConnectorAuthType.OAUTH2, ConnectorAuthType.API_TOKEN, ConnectorAuthType.EXPORT_ONLY),
                        List.of(ConnectorSyncMode.API, ConnectorSyncMode.EXPORT),
                        DEFAULT_FIELD_MAPPINGS.get(provider),
                        DEFAULT_API_SETTINGS.get(provider)
                ))
                .toList();
    }

    public List<ConnectorConfigurationResponse> configurations() {
        String tenantId = TenantContext.getTenantId();
        Map<HrProvider, HrConnectorConfiguration> configured = new EnumMap<>(HrProvider.class);
        configurationRepository.findAllByTenantIdOrderByProviderAsc(tenantId)
                .forEach(configuration -> configured.put(configuration.getProvider(), configuration));

        return Arrays.stream(HrProvider.values())
                .map(provider -> toResponse(configured.getOrDefault(provider, newDefaultConfiguration(tenantId, provider))))
                .toList();
    }

    @Transactional
    public ConnectorConfigurationResponse configure(HrProvider provider, ConnectorConfigurationRequest request) {
        String tenantId = TenantContext.getTenantId();
        HrConnectorConfiguration configuration = configurationRepository.findByTenantIdAndProvider(tenantId, provider)
                .orElseGet(() -> newDefaultConfiguration(tenantId, provider));

        configuration.setEnabled(request.enabled());
        configuration.setAuthType(request.authType() == null ? ConnectorAuthType.EXPORT_ONLY : request.authType());
        configuration.setSyncMode(request.syncMode() == null ? ConnectorSyncMode.EXPORT : request.syncMode());
        configuration.setClientId(blankToNull(request.clientId()));
        configuration.setClientSecret(blankToNull(request.clientSecret()));
        configuration.setApiBaseUrl(blankToNull(request.apiBaseUrl()));
        configuration.setAuthorizationUrl(blankToNull(request.authorizationUrl()));
        configuration.setTokenUrl(blankToNull(request.tokenUrl()));
        configuration.setRedirectUri(blankToNull(request.redirectUri()));
        configuration.setScopes(blankToNull(request.scopes()));
        configuration.setAccessToken(blankToNull(request.accessToken()));
        configuration.setRefreshToken(blankToNull(request.refreshToken()));
        configuration.setTokenExpiresAt(request.tokenExpiresAt());
        configuration.setFieldMappings(normalizeMappings(provider, request.fieldMappings()));
        configuration.setApiSettings(normalizeSettings(provider, request.apiSettings()));
        configuration.setStatus(resolveStatus(configuration));
        configuration.setUpdatedAt(Instant.now());

        return toResponse(configurationRepository.save(configuration));
    }

    public ConnectorAuthorizationResponse authorizationUrl(HrProvider provider) {
        HrConnectorConfiguration configuration = getConfiguration(provider);
        if (configuration.getAuthType() != ConnectorAuthType.OAUTH2) {
            throw new IllegalArgumentException("Connector is not configured for OAuth2");
        }
        if (isBlank(configuration.getAuthorizationUrl()) || isBlank(configuration.getClientId()) || isBlank(configuration.getRedirectUri())) {
            throw new IllegalArgumentException("OAuth2 authorizationUrl, clientId and redirectUri are required");
        }

        String state = "%s:%s:%s".formatted(TenantContext.getTenantId(), provider.name(), UUID.randomUUID());
        String separator = configuration.getAuthorizationUrl().contains("?") ? "&" : "?";
        String authorizationUrl = configuration.getAuthorizationUrl()
                + separator
                + "response_type=code"
                + "&client_id=" + encode(configuration.getClientId())
                + "&redirect_uri=" + encode(configuration.getRedirectUri())
                + "&scope=" + encode(configuration.getScopes() == null ? "" : configuration.getScopes())
                + "&state=" + encode(state);

        return new ConnectorAuthorizationResponse(provider, authorizationUrl, state);
    }

    @Transactional
    public ConnectorSyncResponse sync(HrProvider provider) {
        HrConnectorConfiguration configuration = getConfiguration(provider);
        HrConnectorSyncRun syncRun = new HrConnectorSyncRun();
        syncRun.setTenantId(TenantContext.getTenantId());
        syncRun.setProvider(provider);

        ConnectorStatus status = resolveStatus(configuration);
        if (status != ConnectorStatus.READY) {
            syncRun.setStatus("REJECTED");
            syncRun.setMessage("Connector is not ready: " + status);
            syncRun.setFinishedAt(Instant.now());
            syncRunRepository.save(syncRun);
            configuration.setStatus(status);
            configuration.setLastSyncStatus(syncRun.getStatus());
            configuration.setLastSyncMessage(syncRun.getMessage());
            configuration.setLastSyncAt(syncRun.getFinishedAt());
            configuration.setUpdatedAt(Instant.now());
            configurationRepository.save(configuration);
            return toSyncResponse(syncRun);
        }

        configuration.setStatus(ConnectorStatus.SYNCING);
        configurationRepository.save(configuration);

        if (provider == HrProvider.WORKDAY && configuration.getSyncMode() == ConnectorSyncMode.API) {
            WorkdayConnectorClient.WorkdaySyncResult result = workdayConnectorClient.syncWorkers(configuration);
            syncRun.setStatus("COMPLETED");
            syncRun.setMessage("Workday workers fetched from " + result.sourceUrl());
            syncRun.setImportedRecords(result.importableRecords());
            syncRun.setSkippedRecords(result.skippedRecords());
        } else if (provider == HrProvider.SAP && configuration.getSyncMode() == ConnectorSyncMode.API) {
            SapConnectorClient.SapSyncResult result = sapConnectorClient.syncEmployees(configuration);
            syncRun.setStatus("COMPLETED");
            syncRun.setMessage("SAP SuccessFactors employees fetched from " + result.sourceUrl());
            syncRun.setImportedRecords(result.importableRecords());
            syncRun.setSkippedRecords(result.skippedRecords());
        } else if (provider == HrProvider.FACTORIAL && configuration.getSyncMode() == ConnectorSyncMode.API) {
            FactorialConnectorClient.FactorialSyncResult result = factorialConnectorClient.syncEmployees(configuration);
            syncRun.setStatus("COMPLETED");
            syncRun.setMessage("Factorial employees fetched from " + result.sourceUrl());
            syncRun.setImportedRecords(result.importableRecords());
            syncRun.setSkippedRecords(result.skippedRecords());
        } else if (provider == HrProvider.BAMBOOHR && configuration.getSyncMode() == ConnectorSyncMode.API) {
            BambooHrConnectorClient.BambooHrSyncResult result = bambooHrConnectorClient.syncEmployees(configuration);
            syncRun.setStatus("COMPLETED");
            syncRun.setMessage("BambooHR employees fetched from " + result.sourceUrl());
            syncRun.setImportedRecords(result.importableRecords());
            syncRun.setSkippedRecords(result.skippedRecords());
        } else if (provider == HrProvider.LUCCA && configuration.getSyncMode() == ConnectorSyncMode.API) {
            LuccaConnectorClient.LuccaSyncResult result = luccaConnectorClient.syncEmployees(configuration);
            syncRun.setStatus("COMPLETED");
            syncRun.setMessage("Lucca employees fetched from " + result.sourceUrl());
            syncRun.setImportedRecords(result.importableRecords());
            syncRun.setSkippedRecords(result.skippedRecords());
        } else {
            syncRun.setStatus("COMPLETED");
            syncRun.setMessage("Connector configuration validated. Provider-specific importer can now consume API or export data with the saved mapping.");
            syncRun.setImportedRecords(0);
            syncRun.setSkippedRecords(0);
        }
        syncRun.setFinishedAt(Instant.now());
        syncRunRepository.save(syncRun);

        configuration.setStatus(ConnectorStatus.READY);
        configuration.setLastSyncStatus(syncRun.getStatus());
        configuration.setLastSyncMessage(syncRun.getMessage());
        configuration.setLastSyncAt(syncRun.getFinishedAt());
        configuration.setUpdatedAt(Instant.now());
        configurationRepository.save(configuration);

        return toSyncResponse(syncRun);
    }

    private HrConnectorConfiguration getConfiguration(HrProvider provider) {
        return configurationRepository.findByTenantIdAndProvider(TenantContext.getTenantId(), provider)
                .orElseThrow(() -> new NotFoundException("Connector is not configured"));
    }

    private HrConnectorConfiguration newDefaultConfiguration(String tenantId, HrProvider provider) {
        HrConnectorConfiguration configuration = new HrConnectorConfiguration();
        configuration.setTenantId(tenantId);
        configuration.setProvider(provider);
        configuration.setFieldMappings(normalizeMappings(provider, null));
        configuration.setApiSettings(normalizeSettings(provider, null));
        configuration.setStatus(ConnectorStatus.NEEDS_CONFIGURATION);
        return configuration;
    }

    private ConnectorStatus resolveStatus(HrConnectorConfiguration configuration) {
        if (!configuration.isEnabled()) {
            return ConnectorStatus.DISABLED;
        }
        if (configuration.getFieldMappings().isEmpty()) {
            return ConnectorStatus.NEEDS_CONFIGURATION;
        }
        if (configuration.getSyncMode() == ConnectorSyncMode.API && isBlank(configuration.getApiBaseUrl())) {
            return ConnectorStatus.NEEDS_CONFIGURATION;
        }
        if (configuration.getAuthType() == ConnectorAuthType.OAUTH2) {
            if (isBlank(configuration.getAuthorizationUrl()) || isBlank(configuration.getTokenUrl())
                    || isBlank(configuration.getClientId()) || isBlank(configuration.getRedirectUri())) {
                return ConnectorStatus.NEEDS_CONFIGURATION;
            }
            return hasUsableToken(configuration) ? ConnectorStatus.READY : ConnectorStatus.NEEDS_AUTHORIZATION;
        }
        if (configuration.getAuthType() == ConnectorAuthType.API_TOKEN && isBlank(configuration.getAccessToken())) {
            return ConnectorStatus.NEEDS_AUTHORIZATION;
        }
        return ConnectorStatus.READY;
    }

    private boolean hasUsableToken(HrConnectorConfiguration configuration) {
        if (isBlank(configuration.getAccessToken()) && isBlank(configuration.getRefreshToken())) {
            return false;
        }
        return configuration.getTokenExpiresAt() == null || configuration.getTokenExpiresAt().isAfter(Instant.now());
    }

    private ConnectorConfigurationResponse toResponse(HrConnectorConfiguration configuration) {
        return new ConnectorConfigurationResponse(
                configuration.getId(),
                configuration.getProvider(),
                configuration.getProvider().displayName(),
                configuration.isEnabled(),
                configuration.getAuthType(),
                configuration.getSyncMode(),
                resolveStatus(configuration),
                configuration.getApiBaseUrl(),
                configuration.getAuthorizationUrl(),
                configuration.getTokenUrl(),
                configuration.getRedirectUri(),
                configuration.getScopes(),
                !isBlank(configuration.getClientSecret()),
                !isBlank(configuration.getAccessToken()),
                !isBlank(configuration.getRefreshToken()),
                configuration.getTokenExpiresAt(),
                configuration.getLastSyncAt(),
                configuration.getLastSyncStatus(),
                configuration.getLastSyncMessage(),
                new TreeMap<>(configuration.getFieldMappings()),
                new TreeMap<>(configuration.getApiSettings())
        );
    }

    private ConnectorSyncResponse toSyncResponse(HrConnectorSyncRun syncRun) {
        return new ConnectorSyncResponse(
                syncRun.getId(),
                syncRun.getProvider(),
                syncRun.getStatus(),
                syncRun.getMessage(),
                syncRun.getImportedRecords(),
                syncRun.getSkippedRecords(),
                syncRun.getStartedAt(),
                syncRun.getFinishedAt()
        );
    }

    private Map<String, String> normalizeMappings(HrProvider provider, Map<String, String> mappings) {
        Map<String, String> normalized = new TreeMap<>(DEFAULT_FIELD_MAPPINGS.get(provider));
        if (mappings != null) {
            mappings.forEach((workrhField, providerField) -> {
                if (!isBlank(workrhField) && !isBlank(providerField)) {
                    normalized.put(workrhField.trim(), providerField.trim());
                }
            });
        }
        return normalized;
    }

    private Map<String, String> normalizeSettings(HrProvider provider, Map<String, String> apiSettings) {
        Map<String, String> normalized = new TreeMap<>(DEFAULT_API_SETTINGS.get(provider));
        if (apiSettings != null) {
            apiSettings.forEach((key, value) -> {
                if (!isBlank(key) && !isBlank(value)) {
                    normalized.put(key.trim(), value.trim());
                }
            });
        }
        return normalized;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static Map<HrProvider, Map<String, String>> defaultMappings() {
        Map<HrProvider, Map<String, String>> mappings = new EnumMap<>(HrProvider.class);
        mappings.put(HrProvider.SAP, Map.of(
                "employeeId", "userId",
                "firstName", "firstName",
                "lastName", "lastName",
                "email", "email",
                "department", "department",
                "jobTitle", "title",
                "workDate", "startDate",
                "teleworkDays", "quantity"
        ));
        mappings.put(HrProvider.WORKDAY, Map.of(
                "employeeId", "worker.id",
                "firstName", "worker.firstName",
                "lastName", "worker.lastName",
                "email", "worker.email",
                "department", "worker.department",
                "jobTitle", "worker.jobTitle",
                "workDate", "timeOff.date",
                "teleworkDays", "timeOff.units"
        ));
        mappings.put(HrProvider.FACTORIAL, Map.of(
                "employeeId", "employee_id",
                "firstName", "first_name",
                "lastName", "last_name",
                "email", "email",
                "department", "team.name",
                "jobTitle", "job_title",
                "workDate", "date",
                "teleworkDays", "duration"
        ));
        mappings.put(HrProvider.BAMBOOHR, Map.of(
                "employeeId", "id",
                "firstName", "firstName",
                "lastName", "lastName",
                "email", "workEmail",
                "department", "department",
                "jobTitle", "jobTitle",
                "workDate", "date",
                "teleworkDays", "amount"
        ));
        mappings.put(HrProvider.LUCCA, Map.of(
                "employeeId", "id",
                "firstName", "givenName",
                "lastName", "familyName",
                "email", "email",
                "department", "applicableJobPosition.department.name",
                "jobTitle", "applicableJobPosition.name",
                "workDate", "date",
                "teleworkDays", "duration"
        ));
        mappings.put(HrProvider.PAYFIT, Map.of(
                "employeeId", "employeeId",
                "firstName", "firstName",
                "lastName", "lastName",
                "email", "email",
                "workDate", "periodDate",
                "teleworkDays", "days"
        ));
        return mappings;
    }

    private static Map<HrProvider, Map<String, String>> defaultApiSettings() {
        Map<HrProvider, Map<String, String>> settings = new EnumMap<>(HrProvider.class);
        Arrays.stream(HrProvider.values()).forEach(provider -> settings.put(provider, Map.of()));
        settings.put(HrProvider.SAP, Map.of(
                "employeesPath", "/odata/v2/User",
                "employeesRootPath", "d.results",
                "select", "userId,firstName,lastName,email,department,title",
                "filter", "status eq 't'",
                "top", "100"
        ));
        settings.put(HrProvider.WORKDAY, Map.of(
                "workersPath", "/workers",
                "workersRootPath", "data"
        ));
        settings.put(HrProvider.FACTORIAL, Map.of(
                "employeesPath", "/api/2026-04-01/resources/employees/employees",
                "employeesRootPath", "data",
                "onlyActive", "true"
        ));
        settings.put(HrProvider.BAMBOOHR, Map.of(
                "employeesPath", "/api/v1/employees",
                "employeesRootPath", "data",
                "authMode", "basicApiKey",
                "fields", "firstName,lastName,workEmail,department,jobTitle"
        ));
        settings.put(HrProvider.LUCCA, Map.of(
                "employeesPath", "/lucca-api/employees",
                "employeesRootPath", "items",
                "limit", "100",
                "status", "active"
        ));
        return settings;
    }
}
