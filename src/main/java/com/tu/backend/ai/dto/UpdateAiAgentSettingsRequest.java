package com.tu.backend.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateAiAgentSettingsRequest(
    boolean enabled,
    @Size(max = 1024) String baseUrl,
    @Size(max = 128) String model,
    @Size(max = 4096) String apiKey,
    @Min(1) @Max(3600) Integer connectTimeoutSeconds,
    @Min(1) @Max(7200) Integer readTimeoutSeconds,
    @Min(1) @Max(7200) Integer requestTimeoutSeconds
) {
}
