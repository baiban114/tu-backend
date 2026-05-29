package com.tu.backend.ai.dto;

import java.time.LocalDateTime;

public record AiAgentRunLogSummaryDto(
    String id,
    String taskType,
    String status,
    String baseUrl,
    String model,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    Long durationMs,
    Integer promptTokens,
    Integer completionTokens,
    Integer totalTokens
) {
}
