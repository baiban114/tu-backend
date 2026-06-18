package com.tu.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.common.BusinessException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;

class OpenAiAgentToolLoopClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        AiAgentExecutionContextHolder.clear();
    }

    @Test
    void emitsProgressEventsDuringToolLoop() {
        java.util.ArrayList<AiAgentProgressEvent> events = new java.util.ArrayList<>();
        AiAgentProgressListener listener = new AiAgentProgressListener() {
            @Override
            public void onEvent(AiAgentProgressEvent event) {
                events.add(event);
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        };
        ChatModel chatModel = prompt -> {
            if (prompt.getInstructions().size() <= 2) {
                return responseWithToolCall();
            }
            return response("{\"title\":\"Plan\",\"items\":[{\"title\":\"Step\",\"estimatedHours\":1}]}", usage(10, 5, 15));
        };
        OpenAiCompatibleChatClient client = new OpenAiCompatibleChatClient(
            objectMapper,
            (config, options) -> chatModel,
            ToolCallingManager.builder().build(),
            new AiAgentProperties()
        );

        client.runToolLoop(
            config(),
            "system",
            "user",
            new AiAgentExecutionContext("kb-1", "Java", false),
            listener,
            new StubTools()
        );

        assertThat(events).extracting(AiAgentProgressEvent::phase)
            .contains(
                AiAgentProgressEvent.phaseModelCall(),
                AiAgentProgressEvent.phaseToolCall(),
                AiAgentProgressEvent.phaseToolDone()
            );
    }

    @Test
    void abortsWhenProgressListenerCancelled() {
        AiAgentProgressListener listener = new AiAgentProgressListener() {
            @Override
            public void onEvent(AiAgentProgressEvent event) {
                // no-op
            }

            @Override
            public boolean isCancelled() {
                return true;
            }
        };
        OpenAiCompatibleChatClient client = client((config, options) -> prompt -> {
            throw new IllegalStateException("model should not be called when cancelled");
        });

        assertThatThrownBy(() -> client.runToolLoop(
            config(),
            "system",
            "user",
            new AiAgentExecutionContext("kb-1", "Java", false),
            listener,
            new StubTools()
        ))
            .isInstanceOf(BusinessException.class)
            .hasMessage("ai agent run cancelled");
    }

    @Test
    void returnsFinalContentWithoutToolCalls() {
        OpenAiCompatibleChatClient client = client((config, options) -> prompt -> response("{\"ok\":true}", usage(3, 2, 5)));

        AiAgentToolLoopResult result = client.runToolLoop(
            config(),
            "system",
            "user",
            new AiAgentExecutionContext("kb-1", "Java", false),
            new AiAgentTools(null, null, objectMapper)
        );

        assertThat(result.content()).isEqualTo("{\"ok\":true}");
        assertThat(result.modelCallCount()).isEqualTo(1);
        assertThat(result.toolRoundCount()).isEqualTo(0);
    }

    @Test
    void executesToolLoopUntilModelStopsCallingTools() {
        ChatModel chatModel = prompt -> {
            if (prompt.getInstructions().size() <= 2) {
                return responseWithToolCall();
            }
            return response("{\"title\":\"Plan\",\"items\":[{\"title\":\"Step\",\"estimatedHours\":1}]}", usage(10, 5, 15));
        };
        OpenAiCompatibleChatClient client = new OpenAiCompatibleChatClient(
            objectMapper,
            (config, options) -> chatModel,
            ToolCallingManager.builder().build(),
            new AiAgentProperties()
        );

        AiAgentToolLoopResult result = client.runToolLoop(
            config(),
            "system",
            "user",
            new AiAgentExecutionContext("kb-1", "Java", false),
            new StubTools()
        );

        assertThat(result.content()).contains("\"title\":\"Plan\"");
        assertThat(result.modelCallCount()).isEqualTo(2);
        assertThat(result.toolRoundCount()).isEqualTo(1);
    }

    @Test
    void rejectsWhenMaxToolRoundsExceeded() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getToolLoop().setMaxToolRounds(1);
        OpenAiCompatibleChatClient client = new OpenAiCompatibleChatClient(
            objectMapper,
            (config, options) -> prompt -> responseWithToolCall(),
            ToolCallingManager.builder().build(),
            properties
        );

        assertThatThrownBy(() -> client.runToolLoop(
            config(),
            "system",
            "user",
            new AiAgentExecutionContext(null, "Java", false),
            new StubTools()
        ))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("exceeded max tool loop rounds");
    }

    private OpenAiCompatibleChatClient client(OpenAiCompatibleChatClient.ChatModelFactory factory) {
        return new OpenAiCompatibleChatClient(objectMapper, factory);
    }

    private AiAgentRuntimeConfig config() {
        return new AiAgentRuntimeConfig(true, "https://api.example.com", "sk-secret", "model-a", 30, 300, 300);
    }

    private ChatResponse responseWithToolCall() {
        AssistantMessage message = AssistantMessage.builder()
            .content("")
            .toolCalls(List.of(new AssistantMessage.ToolCall(
                "call-1",
                "function",
                "stubLookup",
                "{\"query\":\"java\"}"
            )))
            .build();
        return new ChatResponse(
            List.of(new Generation(message)),
            ChatResponseMetadata.builder().model("model-a").build()
        );
    }

    private ChatResponse response(String content, Usage usage) {
        return new ChatResponse(
            List.of(new Generation(new AssistantMessage(content))),
            ChatResponseMetadata.builder()
                .model("model-a")
                .usage(usage)
                .build()
        );
    }

    private Usage usage(int promptTokens, int completionTokens, int totalTokens) {
        return new Usage() {
            @Override
            public Integer getPromptTokens() {
                return promptTokens;
            }

            @Override
            public Integer getCompletionTokens() {
                return completionTokens;
            }

            @Override
            public Integer getTotalTokens() {
                return totalTokens;
            }

            @Override
            public Object getNativeUsage() {
                return null;
            }
        };
    }

    static class StubTools {

        @org.springframework.ai.tool.annotation.Tool(description = "Stub lookup tool")
        public String stubLookup(String query) {
            return "{\"query\":\"" + query + "\"}";
        }
    }
}
