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

class LuccaConnectorClientTest {

    private final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    private final LuccaConnectorClient client = new LuccaConnectorClient(restTemplate);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldFetchAndMapLuccaEmployees() throws Exception {
        HrConnectorConfiguration configuration = new HrConnectorConfiguration();
        configuration.setProvider(HrProvider.LUCCA);
        configuration.setApiBaseUrl("https://example.ilucca.net");
        configuration.setAccessToken("access-token");
        configuration.setFieldMappings(Map.of(
                "employeeId", "id",
                "firstName", "givenName",
                "lastName", "familyName",
                "email", "email",
                "department", "applicableJobPosition.department.name",
                "jobTitle", "applicableJobPosition.name"
        ));
        configuration.setApiSettings(Map.of(
                "employeesPath", "/lucca-api/employees",
                "employeesRootPath", "items",
                "limit", "100",
                "status", "active",
                "apiVersion", "latest"
        ));

        JsonNode body = objectMapper.readTree("""
                {
                  "items": [
                    {
                      "id": "416",
                      "givenName": "Ada",
                      "familyName": "Lovelace",
                      "email": "ada@example.com",
                      "applicableJobPosition": {
                        "name": "Engineer",
                        "department": {"name": "Engineering"}
                      }
                    },
                    {
                      "id": "417",
                      "givenName": "Grace",
                      "familyName": "Hopper"
                    }
                  ]
                }
                """);
        when(restTemplate.exchange(
                eq(java.net.URI.create("https://example.ilucca.net/lucca-api/employees?limit=100&status=active")),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(ResponseEntity.ok(body));

        var result = client.syncEmployees(configuration);

        assertThat(result.importableRecords()).isEqualTo(1);
        assertThat(result.skippedRecords()).isEqualTo(1);
        assertThat(result.sourceUrl()).isEqualTo("https://example.ilucca.net/lucca-api/employees?limit=100&status=active");
    }
}
