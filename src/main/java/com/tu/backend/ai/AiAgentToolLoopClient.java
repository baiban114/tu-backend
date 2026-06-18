package com.tu.backend.ai;

public interface AiAgentToolLoopClient {

    default AiAgentToolLoopResult runToolLoop(
        AiAgentRuntimeConfig config,
        String systemPrompt,
        String userPrompt,
        AiAgentExecutionContext executionContext,
        Object... toolProviders
    ) {
        return runToolLoop(config, systemPrompt, userPrompt, executionContext, null, toolProviders);
    }

    AiAgentToolLoopResult runToolLoop(
        AiAgentRuntimeConfig config,
        String systemPrompt,
        String userPrompt,
        AiAgentExecutionContext executionContext,
        AiAgentProgressListener progressListener,
        Object... toolProviders
    );
}
