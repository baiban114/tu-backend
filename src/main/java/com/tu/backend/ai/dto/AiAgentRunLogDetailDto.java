package com.tu.backend.ai.dto;

import java.time.LocalDateTime;

public record AiAgentRunLogDetailDto(
    String id,
    String taskType,
    String status,
    String baseUrl,
    String model,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    Long durationMs,
    String systemPrompt,
    String userPrompt,
    String requestBodyJson,
    String rawResponseBody,
    String outputText,
    String errorMessage,
    Integer promptTokens,
    Integer completionTokens,
    Integer totalTokens
) {
}
