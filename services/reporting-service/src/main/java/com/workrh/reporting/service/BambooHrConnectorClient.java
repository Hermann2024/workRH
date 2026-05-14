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
public class BambooHrConnectorClient {

    private static final String DEFAULT_EMPLOYEES_PATH = "/api/v1/employees";

    private final RestTemplate restTemplate;

    public BambooHrConnectorClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public BambooHrSyncResult syncEmployees(HrConnectorConfiguration configuration) {
        URI employeesUri = employeesUri(configuration);
        HttpHeaders headers = new HttpHeaders();
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
            BambooHrEmployee mapped = mapEmployee(employee, configuration.getFieldMappings());
            if (mapped.isImportable()) {
                importable++;
            } else {
                skipped++;
            }
        }
        return new BambooHrSyncResult(importable, skipped, employeesUri.toString());
    }

    private URI employeesUri(HrConnectorConfiguration configuration) {
        String employeesPath = configuration.getApiSettings().getOrDefault("employeesPath", DEFAULT_EMPLOYEES_PATH);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(configuration.getApiBaseUrl())
                .path(employeesPath.startsWith("/") ? employeesPath : "/" + employeesPath);
        String fields = configuration.getApiSettings().get("fields");
        if (fields != null && !fields.isBlank()) {
            builder.queryParam("fields", fields);
        }
        return builder.build().toUri();
    }

    private void applyAuthentication(HrConnectorConfiguration configuration, HttpHeaders headers) {
        String token = configuration.getAccessToken();
        if (token == null || token.isBlank()) {
            return;
        }
        String authMode = configuration.getApiSettings().getOrDefault("authMode", "basicApiKey");
        if ("bearer".equalsIgnoreCase(authMode)) {
            headers.setBearerAuth(token);
            return;
        }
        String credentials = token + ":x";
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8)));
    }

    private List<JsonNode> resolveEmployees(JsonNode body, Map<String, String> apiSettings) {
        if (body == null || body.isNull()) {
            return List.of();
        }

        JsonNode employeesNode = body;
        String employeesRootPath = apiSettings.get("employeesRootPath");
        if (employeesRootPath != null && !employeesRootPath.isBlank()) {
            employeesNode = readPath(body, employeesRootPath);
        } else if (body.has("data")) {
            employeesNode = body.get("data");
        } else if (body.has("employees")) {
            employeesNode = body.get("employees");
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

    private BambooHrEmployee mapEmployee(JsonNode employee, Map<String, String> fieldMappings) {
        return new BambooHrEmployee(
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

    public record BambooHrSyncResult(int importableRecords, int skippedRecords, String sourceUrl) {
    }

    private record BambooHrEmployee(
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
