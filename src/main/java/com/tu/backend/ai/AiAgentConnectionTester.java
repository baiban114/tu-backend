package com.tu.backend.ai;

public interface AiAgentConnectionTester {

    String completeJson(AiAgentRuntimeConfig config, String systemPrompt, String userPrompt);
}
