package com.tu.backend.ai;

public interface AiChatClient {

    String completeJson(String systemPrompt, String userPrompt);
}
