package com.tu.backend.ai;

public record AiChatCompletionResult(
    String content,
    String requestBodyJson,
    String rawResponseBody,
    Long durationMs,
    Integer promptTokens,
    Integer completionTokens,
    Integer totalTokens
) {
}
