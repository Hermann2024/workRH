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

class FactorialConnectorClientTest {

    private final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    private final FactorialConnectorClient client = new FactorialConnectorClient(restTemplate);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldFetchAndMapFactorialEmployees() throws Exception {
        HrConnectorConfiguration configuration = new HrConnectorConfiguration();
        configuration.setProvider(HrProvider.FACTORIAL);
        configuration.setApiBaseUrl("https://api.factorialhr.com");
        configuration.setAccessToken("access-token");
        configuration.setFieldMappings(Map.of(
                "employeeId", "id",
                "firstName", "first_name",
                "lastName", "last_name",
                "email", "email",
                "department", "team.name",
                "jobTitle", "job_title"
        ));
        configuration.setApiSettings(Map.of(
                "employeesPath", "/api/2026-04-01/resources/employees/employees",
                "employeesRootPath", "data",
                "onlyActive", "true"
        ));

        JsonNode body = objectMapper.readTree("""
                {
                  "data": [
                    {
                      "id": 1001,
                      "first_name": "Ada",
                      "last_name": "Lovelace",
                      "email": "ada@example.com",
                      "team": {"name": "Engineering"},
                      "job_title": "Engineer"
                    },
                    {
                      "id": 1002,
                      "first_name": "Grace",
                      "last_name": "Hopper"
                    }
                  ]
                }
                """);
        when(restTemplate.exchange(
                eq(java.net.URI.create("https://api.factorialhr.com/api/2026-04-01/resources/employees/employees?only_active=true")),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(ResponseEntity.ok(body));

        var result = client.syncEmployees(configuration);

        assertThat(result.importableRecords()).isEqualTo(1);
        assertThat(result.skippedRecords()).isEqualTo(1);
        assertThat(result.sourceUrl()).isEqualTo("https://api.factorialhr.com/api/2026-04-01/resources/employees/employees?only_active=true");
    }
}
