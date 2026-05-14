package com.workrh.reporting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.workrh.reporting.domain.HrConnectorConfiguration;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SapConnectorClient {

    private static final String DEFAULT_EMPLOYEES_PATH = "/odata/v2/User";

    private final RestTemplate restTemplate;

    public SapConnectorClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public SapSyncResult syncEmployees(HrConnectorConfiguration configuration) {
        URI employeesUri = employeesUri(configuration);
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "application/json");
        applyAuthentication(configuration, headers);

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                employeesUri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class
        );

        List<JsonNode> employees = resolveEmployees(response.getBody(), configuration.getApiSettings());
        int importable = 0;
        int skipped = 0;
        for (JsonNode employee : employees) {
            SapEmployee mapped = mapEmployee(employee, configuration.getFieldMappings());
            if (mapped.isImportable()) {
                importable++;
            } else {
                skipped++;
            }
        }
        return new SapSyncResult(importable, skipped, employeesUri.toString());
    }

    private URI employeesUri(HrConnectorConfiguration configuration) {
        String employeesPath = configuration.getApiSettings().getOrDefault("employeesPath", DEFAULT_EMPLOYEES_PATH);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(configuration.getApiBaseUrl())
                .path(employeesPath.startsWith("/") ? employeesPath : "/" + employeesPath)
                .queryParam("$format", "json");
        String select = configuration.getApiSettings().get("select");
        if (select != null && !select.isBlank()) {
            builder.queryParam("$select", select);
        }
        String filter = configuration.getApiSettings().get("filter");
        if (filter != null && !filter.isBlank()) {
            builder.queryParam("$filter", filter);
        }
        String top = configuration.getApiSettings().get("top");
        if (top != null && !top.isBlank()) {
            builder.queryParam("$top", top);
        }
        return builder.build().toUri();
    }

    private void applyAuthentication(HrConnectorConfiguration configuration, HttpHeaders headers) {
        String token = configuration.getAccessToken();
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
            return;
        }
        if (configuration.getClientId() != null && !configuration.getClientId().isBlank()
                && configuration.getClientSecret() != null && !configuration.getClientSecret().isBlank()) {
            String credentials = configuration.getClientId() + ":" + configuration.getClientSecret();
            headers.set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private List<JsonNode> resolveEmployees(JsonNode body, Map<String, String> apiSettings) {
        if (body == null || body.isNull()) {
            return List.of();
        }

        JsonNode employeesNode = body;
        String employeesRootPath = apiSettings.get("employeesRootPath");
        if (employeesRootPath != null && !employeesRootPath.isBlank()) {
            employeesNode = readPath(body, employeesRootPath);
        } else if (body.has("d") && body.get("d").has("results")) {
            employeesNode = body.get("d").get("results");
        } else if (body.has("value")) {
            employeesNode = body.get("value");
        } else if (body.has("data")) {
            employeesNode = body.get("data");
        }

        if (employeesNode == null || !employeesNode.isArray()) {
            return List.of();
        }

        List<JsonNode> employees = new ArrayList<>();
        Iterator<JsonNode> iterator = employeesNode.elements();
        while (iterator.hasNext()) {
            employees.add(iterator.next());
        }
        return employees;
    }

    private SapEmployee mapEmployee(JsonNode employee, Map<String, String> fieldMappings) {
        return new SapEmployee(
                readText(employee, fieldMappings.get("employeeId")),
                readText(employee, fieldMappings.get("firstName")),
                readText(employee, fieldMappings.get("lastName")),
                readText(employee, fieldMappings.get("email")),
                readText(employee, fieldMappings.get("department")),
                readText(employee, fieldMappings.get("jobTitle"))
        );
    }

    private String readText(JsonNode root, String path) {
        JsonNode value = readPath(root, path);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        return text == null || text.isBlank() ? null : text.trim();
    }

    private JsonNode readPath(JsonNode root, String path) {
        if (root == null || path == null || path.isBlank()) {
            return null;
        }
        JsonNode current = root;
        for (String segment : path.split("\\.")) {
            if (current == null || segment.isBlank()) {
                return null;
            }
            current = current.get(segment);
        }
        return current;
    }

    public record SapSyncResult(int importableRecords, int skippedRecords, String sourceUrl) {
    }

    private record SapEmployee(
            String employeeId,
            String firstName,
            String lastName,
            String email,
            String department,
            String jobTitle
    ) {
        boolean isImportable() {
            return email != null && firstName != null && lastName != null;
        }
    }
}
