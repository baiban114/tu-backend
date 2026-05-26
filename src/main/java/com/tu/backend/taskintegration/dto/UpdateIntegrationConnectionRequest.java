package com.tu.backend.taskintegration.dto;

import jakarta.validation.constraints.Size;

public record UpdateIntegrationConnectionRequest(
    @Size(max = 1024) String baseUrl,
    @Size(max = 2048) String apiKey,
    @Size(max = 128) String workspaceId,
    String adapterProfileJson,
    Boolean enabled
) {
}
