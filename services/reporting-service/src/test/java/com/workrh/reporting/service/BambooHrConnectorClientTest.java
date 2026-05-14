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

class BambooHrConnectorClientTest {

    private final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    private final BambooHrConnectorClient client = new BambooHrConnectorClient(restTemplate);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldFetchAndMapBambooHrEmployees() throws Exception {
        HrConnectorConfiguration configuration = new HrConnectorConfiguration();
        configuration.setProvider(HrProvider.BAMBOOHR);
        configuration.setApiBaseUrl("https://acme.bamboohr.com");
        configuration.setAccessToken("api-key");
        configuration.setFieldMappings(Map.of(
                "employeeId", "id",
                "firstName", "firstName",
                "lastName", "lastName",
                "email", "workEmail",
                "department", "department",
                "jobTitle", "jobTitle"
        ));
        configuration.setApiSettings(Map.of(
                "employeesPath", "/api/v1/employees",
                "employeesRootPath", "data",
                "authMode", "basicApiKey",
                "fields", "firstName,lastName,workEmail,department,jobTitle"
        ));

        JsonNode body = objectMapper.readTree("""
                {
                  "data": [
                    {
                      "id": "42",
                      "firstName": "Ada",
                      "lastName": "Lovelace",
                      "workEmail": "ada@example.com",
                      "department": "Engineering",
                      "jobTitle": "Engineer"
                    },
                    {
                      "id": "77",
                      "firstName": "Grace",
                      "lastName": "Hopper"
                    }
                  ]
                }
                """);
        when(restTemplate.exchange(
                eq(java.net.URI.create("https://acme.bamboohr.com/api/v1/employees?fields=firstName,lastName,workEmail,department,jobTitle")),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(ResponseEntity.ok(body));

        var result = client.syncEmployees(configuration);

        assertThat(result.importableRecords()).isEqualTo(1);
        assertThat(result.skippedRecords()).isEqualTo(1);
        assertThat(result.sourceUrl()).isEqualTo("https://acme.bamboohr.com/api/v1/employees?fields=firstName,lastName,workEmail,department,jobTitle");
    }
}
