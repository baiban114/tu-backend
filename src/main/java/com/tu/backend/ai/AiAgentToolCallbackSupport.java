package com.tu.backend.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

final class AiAgentToolCallbackSupport {

    private AiAgentToolCallbackSupport() {
    }

    static List<ToolCallback> fromProviders(Object... toolProviders) {
        if (toolProviders == null || toolProviders.length == 0) {
            return List.of();
        }
        Object[] providers = Arrays.stream(toolProviders)
            .filter(Objects::nonNull)
            .toArray();
        if (providers.length == 0) {
            return List.of();
        }
        return new ArrayList<>(Arrays.asList(
            MethodToolCallbackProvider.builder()
                .toolObjects(providers)
                .build()
                .getToolCallbacks()
        ));
    }
}
