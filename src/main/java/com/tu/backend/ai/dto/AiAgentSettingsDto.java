package com.tu.backend.ai.dto;

public record AiAgentSettingsDto(
    boolean enabled,
    String baseUrl,
    String model,
    boolean apiKeyConfigured,
    int connectTimeoutSeconds,
    int readTimeoutSeconds,
    int requestTimeoutSeconds
) {
}
