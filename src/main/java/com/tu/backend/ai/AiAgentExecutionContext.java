package com.tu.backend.ai;

public record AiAgentExecutionContext(
    String kbId,
    String topic,
    boolean enableWebSearch
) {
}
