package com.tu.backend.ai;

public record AiAgentRuntimeConfig(
    boolean enabled,
    String baseUrl,
    String apiKey,
    String model,
    int connectTimeoutSeconds,
    int readTimeoutSeconds,
    int requestTimeoutSeconds
) {
}
