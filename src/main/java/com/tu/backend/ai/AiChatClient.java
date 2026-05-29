package com.tu.backend.ai;

public interface AiChatClient {

    AiChatCompletionResult completeJson(AiAgentRuntimeConfig config, String systemPrompt, String userPrompt);
}
