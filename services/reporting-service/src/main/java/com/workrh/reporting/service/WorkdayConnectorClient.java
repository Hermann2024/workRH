package com.workrh.reporting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.workrh.reporting.domain.HrConnectorConfiguration;
import java.net.URI;
import java.util.ArrayList;
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
public class WorkdayConnectorClient {

    private static final String DEFAULT_WORKERS_PATH = "/workers";

    private final RestTemplate restTemplate;

    public WorkdayConnectorClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public WorkdaySyncResult syncWorkers(HrConnectorConfiguration configuration) {
        URI workersUri = workersUri(configuration);
        HttpHeaders headers = new HttpHeaders();
        if (configuration.getAccessToken() != null && !configuration.getAccessToken().isBlank()) {
            headers.setBearerAuth(configuration.getAccessToken());
        }

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                workersUri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                JsonNode.class
        );

        List<JsonNode> workers = resolveWorkers(response.getBody(), configuration.getApiSettings());
        int importable = 0;
        int skipped = 0;
        for (JsonNode worker : workers) {
            WorkdayWorker mapped = mapWorker(worker, configuration.getFieldMappings());
            if (mapped.isImportable()) {
                importable++;
            } else {
                skipped++;
            }
        }
        return new WorkdaySyncResult(importable, skipped, workersUri.toString());
    }

    private URI workersUri(HrConnectorConfiguration configuration) {
        String workersPath = configuration.getApiSettings().getOrDefault("workersPath", DEFAULT_WORKERS_PATH);
        return UriComponentsBuilder.fromHttpUrl(configuration.getApiBaseUrl())
                .path(workersPath.startsWith("/") ? workersPath : "/" + workersPath)
                .build()
                .toUri();
    }

    private List<JsonNode> resolveWorkers(JsonNode body, Map<String, String> apiSettings) {
        if (body == null || body.isNull()) {
            return List.of();
        }

        JsonNode workersNode = body;
        String workersRootPath = apiSettings.get("workersRootPath");
        if (workersRootPath != null && !workersRootPath.isBlank()) {
            workersNode = readPath(body, workersRootPath);
        } else if (body.has("data")) {
            workersNode = body.get("data");
        } else if (body.has("workers")) {
            workersNode = body.get("workers");
        } else if (body.has("Report_Entry")) {
            workersNode = body.get("Report_Entry");
        }

        if (workersNode == null || !workersNode.isArray()) {
            return List.of();
        }

        List<JsonNode> workers = new ArrayList<>();
        Iterator<JsonNode> iterator = workersNode.elements();
        while (iterator.hasNext()) {
            workers.add(iterator.next());
        }
        return workers;
    }

    private WorkdayWorker mapWorker(JsonNode worker, Map<String, String> fieldMappings) {
        return new WorkdayWorker(
                readText(worker, fieldMappings.get("employeeId")),
                readText(worker, fieldMappings.get("firstName")),
                readText(worker, fieldMappings.get("lastName")),
                readText(worker, fieldMappings.get("email")),
                readText(worker, fieldMappings.get("department")),
                readText(worker, fieldMappings.get("jobTitle"))
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

    public record WorkdaySyncResult(int importableRecords, int skippedRecords, String sourceUrl) {
    }

    private record WorkdayWorker(
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
