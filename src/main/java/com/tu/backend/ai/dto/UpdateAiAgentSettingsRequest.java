package com.tu.backend.ai.dto;

import jakarta.validation.constraints.Size;

public record UpdateAiAgentSettingsRequest(
    boolean enabled,
    @Size(max = 1024) String baseUrl,
    @Size(max = 128) String model,
    @Size(max = 4096) String apiKey
) {
}
