package com.tu.backend.ai;

public record AiAgentToolLoopResult(
    String content,
    String requestBodyJson,
    String rawResponseBody,
    Long durationMs,
    Integer promptTokens,
    Integer completionTokens,
    Integer totalTokens,
    int modelCallCount,
    int toolRoundCount
) {

    public AiChatCompletionResult toCompletionResult() {
        return new AiChatCompletionResult(
            content,
            requestBodyJson,
            rawResponseBody,
            durationMs,
            promptTokens,
            completionTokens,
            totalTokens
        );
    }
}
