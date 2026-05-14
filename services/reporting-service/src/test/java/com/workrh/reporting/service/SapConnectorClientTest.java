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

class SapConnectorClientTest {

    private final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
    private final SapConnectorClient client = new SapConnectorClient(restTemplate);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldFetchAndMapSapODataEmployees() throws Exception {
        HrConnectorConfiguration configuration = new HrConnectorConfiguration();
        configuration.setProvider(HrProvider.SAP);
        configuration.setApiBaseUrl("https://api.successfactors.example");
        configuration.setAccessToken("access-token");
        configuration.setFieldMappings(Map.of(
                "employeeId", "userId",
                "firstName", "firstName",
                "lastName", "lastName",
                "email", "email",
                "department", "department",
                "jobTitle", "title"
        ));
        configuration.setApiSettings(Map.of(
                "employeesPath", "/odata/v2/User",
                "employeesRootPath", "d.results",
                "select", "userId,firstName,lastName,email,department,title",
                "filter", "status eq 't'",
                "top", "100"
        ));

        JsonNode body = objectMapper.readTree("""
                {
                  "d": {
                    "results": [
                      {
                        "userId": "1001",
                        "firstName": "Ada",
                        "lastName": "Lovelace",
                        "email": "ada@example.com",
                        "department": "Engineering",
                        "title": "Engineer"
                      },
                      {
                        "userId": "1002",
                        "firstName": "Grace",
                        "lastName": "Hopper"
                      }
                    ]
                  }
                }
                """);
        when(restTemplate.exchange(
                eq(java.net.URI.create("https://api.successfactors.example/odata/v2/User?$format=json&$select=userId,firstName,lastName,email,department,title&$filter=status%20eq%20't'&$top=100")),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(JsonNode.class)
        )).thenReturn(ResponseEntity.ok(body));

        var result = client.syncEmployees(configuration);

        assertThat(result.importableRecords()).isEqualTo(1);
        assertThat(result.skippedRecords()).isEqualTo(1);
        assertThat(result.sourceUrl()).isEqualTo("https://api.successfactors.example/odata/v2/User?$format=json&$select=userId,firstName,lastName,email,department,title&$filter=status%20eq%20't'&$top=100");
    }
}
