package com.tu.backend.taskintegration;

import com.tu.backend.common.ApiResponse;
import com.tu.backend.common.BusinessException;
import com.tu.backend.taskintegration.dto.CreateExternalTaskRequest;
import com.tu.backend.taskintegration.dto.ExternalProjectDto;
import com.tu.backend.taskintegration.dto.ExternalProviderDto;
import com.tu.backend.taskintegration.dto.ExternalTaskDto;
import com.tu.backend.taskintegration.dto.MoveExternalTaskRequest;
import com.tu.backend.taskintegration.dto.UpdateExternalTaskRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class IntegrationClient {

    private final IntegrationProperties properties;
    private final RestClient.Builder restClientBuilder;

    public IntegrationClient(IntegrationProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClientBuilder = restClientBuilder;
    }

    public List<ExternalProviderDto> providers(KaneoConnectionContext connection) {
        return getList("/internal/task-integrations/providers", connection, new ParameterizedTypeReference<>() {});
    }

    public List<ExternalProjectDto> projects(KaneoConnectionContext connection) {
        return getList("/internal/task-integrations/projects", connection, new ParameterizedTypeReference<>() {});
    }

    public List<ExternalTaskDto> tasks(KaneoConnectionContext connection, String projectId, String status, String priority, String assigneeId, String page, String limit, String sortBy, String sortOrder) {
        String query = query(
            "status", status,
            "priority", priority,
            "assigneeId", assigneeId,
            "page", page,
            "limit", limit,
            "sortBy", sortBy,
            "sortOrder", sortOrder
        );
        return getList("/internal/task-integrations/projects/" + encode(projectId) + "/tasks" + query, connection, new ParameterizedTypeReference<>() {});
    }

    public ExternalTaskDto createTask(KaneoConnectionContext connection, String projectId, CreateExternalTaskRequest request) {
        return unwrap(client().post()
            .uri("/internal/task-integrations/projects/{projectId}/tasks", projectId)
            .headers(headers -> applyConnectionHeaders(headers, connection))
            .body(request)
            .retrieve()
            .body(new ParameterizedTypeReference<ApiResponse<ExternalTaskDto>>() {}));
    }

    public ExternalTaskDto updateTask(KaneoConnectionContext connection, String taskId, UpdateExternalTaskRequest request) {
        return unwrap(client().patch()
            .uri("/internal/task-integrations/tasks/{taskId}", taskId)
            .headers(headers -> applyConnectionHeaders(headers, connection))
            .body(request)
            .retrieve()
            .body(new ParameterizedTypeReference<ApiResponse<ExternalTaskDto>>() {}));
    }

    public ExternalTaskDto moveTask(KaneoConnectionContext connection, String taskId, MoveExternalTaskRequest request) {
        return unwrap(client().post()
            .uri("/internal/task-integrations/tasks/{taskId}/move", taskId)
            .headers(headers -> applyConnectionHeaders(headers, connection))
            .body(request)
            .retrieve()
            .body(new ParameterizedTypeReference<ApiResponse<ExternalTaskDto>>() {}));
    }

    private <T> List<T> getList(String path, KaneoConnectionContext connection, ParameterizedTypeReference<ApiResponse<List<T>>> type) {
        return unwrap(client().get()
            .uri(path)
            .headers(headers -> applyConnectionHeaders(headers, connection))
            .retrieve()
            .body(type));
    }

    private <T> T unwrap(ApiResponse<T> response) {
        if (response == null) throw new BusinessException(50200, "integration service returned empty response");
        if (response.code() != 0) throw new BusinessException(response.code(), response.message());
        return response.data();
    }

    private RestClient client() {
        try {
            return restClientBuilder.baseUrl(stripTrailingSlash(properties.getServiceUrl())).build();
        } catch (RestClientException ex) {
            throw new BusinessException(50200, "integration service unavailable");
        }
    }

    private void applyConnectionHeaders(org.springframework.http.HttpHeaders headers, KaneoConnectionContext connection) {
        headers.set("X-Tu-Kaneo-Base-Url", connection.baseUrl());
        headers.set("X-Tu-Kaneo-Api-Key", connection.apiKey());
        if (connection.workspaceId() != null && !connection.workspaceId().isBlank()) {
            headers.set("X-Tu-Kaneo-Workspace-Id", connection.workspaceId());
        }
        if (connection.adapterProfileJson() != null && !connection.adapterProfileJson().isBlank()) {
            headers.set("X-Tu-Adapter-Profile", connection.adapterProfileJson().replaceAll("\\s+", " ").trim());
        }
    }

    private String query(String... pairs) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            String value = pairs[i + 1];
            if (value == null || value.isBlank()) continue;
            builder.append(builder.isEmpty() ? '?' : '&')
                .append(pairs[i])
                .append('=')
                .append(encode(value));
        }
        return builder.toString();
    }

    private String encode(String value) {
        return value == null ? "" : value.replace(" ", "%20");
    }

    private String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) return "";
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
