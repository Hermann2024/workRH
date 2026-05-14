package com.workrh.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workrh.reporting.domain.HrConnectorConfiguration;
import com.workrh.reporting.domain.HrProvider;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class WorkdayConnectorClientTest {

    private final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    private final WorkdayConnectorClient client = new WorkdayConnectorClient(restTemplate);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldFetchAndMapWorkdayWorkers() throws Exception {
        HrConnectorConfiguration configuration = new HrConnectorConfiguration();
        configuration.setProvider(HrProvider.WORKDAY);
        configuration.setApiBaseUrl("https://tenant.workday.test/api");
        configuration.setAccessToken("access-token");
        configuration.setFieldMappings(Map.of(
                "employeeId", "worker.id",
                "firstName", "worker.firstName",
                "lastName", "worker.lastName",
                "email", "worker.email"
        ));
        configuration.setApiSettings(Map.of(
                "workersPath", "/workers",
                "workersRootPath", "data"
        ));

        JsonNode body = objectMapper.readTree("""
                {
                  "data": [
                    {"worker": {"id": "W1", "firstName": "Ada", "lastName": "Lovelace", "email": "ada@example.com"}},
                    {"worker": {"id": "W2", "firstName": "Grace", "lastName": "Hopper"}}
                  ]
                }
                """);
        when(restTemplate.exchange(
                eq(java.net.URI.create("https://tenant.workday.test/api/workers")),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(ResponseEntity.ok(body));

        var result = client.syncWorkers(configuration);

        assertThat(result.importableRecords()).isEqualTo(1);
        assertThat(result.skippedRecords()).isEqualTo(1);
        assertThat(result.sourceUrl()).isEqualTo("https://tenant.workday.test/api/workers");
    }
}
