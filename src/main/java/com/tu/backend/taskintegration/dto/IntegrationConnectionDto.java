package com.tu.backend.taskintegration.dto;

public record IntegrationConnectionDto(
    String id,
    String provider,
    String baseUrl,
    String workspaceId,
    String adapterProfileJson,
    boolean apiKeyConfigured,
    boolean enabled
) {
}
