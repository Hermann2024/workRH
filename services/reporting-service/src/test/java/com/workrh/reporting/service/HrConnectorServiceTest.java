package com.workrh.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.workrh.common.tenant.TenantContext;
import com.workrh.reporting.api.dto.ConnectorConfigurationRequest;
import com.workrh.reporting.domain.ConnectorAuthType;
import com.workrh.reporting.domain.ConnectorStatus;
import com.workrh.reporting.domain.ConnectorSyncMode;
import com.workrh.reporting.domain.HrConnectorConfiguration;
import com.workrh.reporting.domain.HrConnectorSyncRun;
import com.workrh.reporting.domain.HrProvider;
import com.workrh.reporting.repository.HrConnectorConfigurationRepository;
import com.workrh.reporting.repository.HrConnectorSyncRunRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class HrConnectorServiceTest {

    private final HrConnectorConfigurationRepository configurationRepository = Mockito.mock(HrConnectorConfigurationRepository.class);
    private final HrConnectorSyncRunRepository syncRunRepository = Mockito.mock(HrConnectorSyncRunRepository.class);
    private final WorkdayConnectorClient workdayConnectorClient = Mockito.mock(WorkdayConnectorClient.class);
    private final FactorialConnectorClient factorialConnectorClient = Mockito.mock(FactorialConnectorClient.class);
    private final BambooHrConnectorClient bambooHrConnectorClient = Mockito.mock(BambooHrConnectorClient.class);
    private final LuccaConnectorClient luccaConnectorClient = Mockito.mock(LuccaConnectorClient.class);
    private final SapConnectorClient sapConnectorClient = Mockito.mock(SapConnectorClient.class);
    private final HrConnectorService service = new HrConnectorService(
            configurationRepository,
            syncRunRepository,
            workdayConnectorClient,
            factorialConnectorClient,
            bambooHrConnectorClient,
            luccaConnectorClient,
            sapConnectorClient
    );

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    @Test
    void shouldExposeCatalogForTargetProviders() {
        List<String> providers = service.catalog().stream()
                .map(item -> item.displayName())
                .toList();

        assertThat(providers).containsExactly("SAP", "Workday", "Factorial", "BambooHR", "Lucca", "Payfit");
    }

    @Test
    void shouldMarkOauthConnectorAsNeedingAuthorizationWithoutToken() {
        TenantContext.setTenantId("tenant-a");
        when(configurationRepository.findByTenantIdAndProvider("tenant-a", HrProvider.LUCCA)).thenReturn(Optional.empty());
        when(configurationRepository.save(any(HrConnectorConfiguration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.configure(HrProvider.LUCCA, new ConnectorConfigurationRequest(
                true,
                ConnectorAuthType.OAUTH2,
                ConnectorSyncMode.API,
                "client-id",
                "secret",
                "https://example.lucca.test",
                "https://example.lucca.test/oauth/authorize",
                "https://example.lucca.test/oauth/token",
                "https://workrh.test/callback",
                "employees leaves",
                null,
                null,
                null,
                Map.of("employeeId", "owner.id"),
                Map.of()
        ));

        assertThat(response.status()).isEqualTo(ConnectorStatus.NEEDS_AUTHORIZATION);
        assertThat(response.hasClientSecret()).isTrue();
        assertThat(response.hasAccessToken()).isFalse();
    }

    @Test
    void shouldBuildOauthAuthorizationUrl() {
        TenantContext.setTenantId("tenant-a");
        HrConnectorConfiguration configuration = readyOauthConfiguration();
        configuration.setAccessToken(null);
        configuration.setRefreshToken(null);
        when(configurationRepository.findByTenantIdAndProvider("tenant-a", HrProvider.FACTORIAL))
                .thenReturn(Optional.of(configuration));

        var response = service.authorizationUrl(HrProvider.FACTORIAL);

        assertThat(response.provider()).isEqualTo(HrProvider.FACTORIAL);
        assertThat(response.authorizationUrl()).contains("response_type=code");
        assertThat(response.authorizationUrl()).contains("client_id=client-id");
        assertThat(response.authorizationUrl()).contains("redirect_uri=https%3A%2F%2Fworkrh.test%2Fcallback");
        assertThat(response.state()).startsWith("tenant-a:FACTORIAL:");
    }

    @Test
    void shouldRejectSyncWhenConnectorNeedsAuthorization() {
        TenantContext.setTenantId("tenant-a");
        HrConnectorConfiguration configuration = readyOauthConfiguration();
        configuration.setAccessToken(null);
        configuration.setRefreshToken(null);
        when(configurationRepository.findByTenantIdAndProvider("tenant-a", HrProvider.FACTORIAL))
                .thenReturn(Optional.of(configuration));
        when(syncRunRepository.save(any(HrConnectorSyncRun.class))).thenAnswer(invocation -> {
            HrConnectorSyncRun syncRun = invocation.getArgument(0);
            syncRun.setId(99L);
            return syncRun;
        });
        when(configurationRepository.save(any(HrConnectorConfiguration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.sync(HrProvider.FACTORIAL);

        assertThat(response.syncRunId()).isEqualTo(99L);
        assertThat(response.status()).isEqualTo("REJECTED");
        assertThat(response.message()).contains("NEEDS_AUTHORIZATION");
    }

    private HrConnectorConfiguration readyOauthConfiguration() {
        HrConnectorConfiguration configuration = new HrConnectorConfiguration();
        configuration.setTenantId("tenant-a");
        configuration.setProvider(HrProvider.FACTORIAL);
        configuration.setEnabled(true);
        configuration.setAuthType(ConnectorAuthType.OAUTH2);
        configuration.setSyncMode(ConnectorSyncMode.API);
        configuration.setClientId("client-id");
        configuration.setClientSecret("secret");
        configuration.setApiBaseUrl("https://api.factorial.test");
        configuration.setAuthorizationUrl("https://api.factorial.test/oauth/authorize");
        configuration.setTokenUrl("https://api.factorial.test/oauth/token");
        configuration.setRedirectUri("https://workrh.test/callback");
        configuration.setScopes("employees leaves");
        configuration.setAccessToken("access");
        configuration.setFieldMappings(Map.of("employeeId", "employee_id"));
        return configuration;
    }
}
