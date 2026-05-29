package com.tu.backend.ai;

public interface AiAgentConnectionTester {

    AiChatCompletionResult completeJson(AiAgentRuntimeConfig config, String systemPrompt, String userPrompt);
}
