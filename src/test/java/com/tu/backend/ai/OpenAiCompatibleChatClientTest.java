package com.tu.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.Timeout;
import com.tu.backend.common.BusinessException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class OpenAiCompatibleChatClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void rejectsDisabledAgent() {
        OpenAiCompatibleChatClient client = client((config, options) -> responseModel("{\"ok\":true}", usage(1, 1, 2)));

        assertThatThrownBy(() -> client.completeJson(new AiAgentRuntimeConfig(false, "", "", "", 30, 300, 300), "system", "user"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("ai agent disabled");
    }

    @Test
    void canBeCreatedBySpringContainer() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(ObjectMapper.class, (Supplier<ObjectMapper>) ObjectMapper::new);
            context.registerBean(AiAgentProperties.class, AiAgentProperties::new);
            context.registerBean(ToolCallingManager.class, () -> ToolCallingManager.builder().build());
            context.register(OpenAiCompatibleChatClient.class);
            context.refresh();

            assertThat(context.getBean(OpenAiCompatibleChatClient.class)).isNotNull();
            assertThat(context.getBean(AiChatClient.class)).isNotNull();
            assertThat(context.getBean(AiAgentToolLoopClient.class)).isNotNull();
            assertThat(context.getBean(AiAgentConnectionTester.class)).isNotNull();
        }
    }

    @Test
    void rejectsIncompleteConfiguration() {
        OpenAiCompatibleChatClient client = client((config, options) -> responseModel("{\"ok\":true}", usage(1, 1, 2)));

        assertThatThrownBy(() -> client.completeJson(new AiAgentRuntimeConfig(true, "https://api.example.com/v1", "", "model", 30, 300, 300), "system", "user"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("ai agent configuration incomplete");
    }

    @Test
    void wrapsModelFailuresWithDetailedContext() {
        OpenAiCompatibleChatClient client = client((config, options) -> prompt -> {
            throw new IllegalArgumentException("synthetic spring ai failure");
        });

        assertThatThrownBy(() -> client.completeJson(new AiAgentRuntimeConfig(true, "https://api.example.com", "sk-secret", "model-a", 30, 300, 300), "system", "user"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ai agent request failed")
            .hasMessageContaining("POST https://api.example.com/chat/completions")
            .hasMessageContaining("model=model-a")
            .hasMessageContaining("java.lang.IllegalArgumentException")
            .hasMessageContaining("synthetic spring ai failure")
            .hasMessageNotContaining("sk-secret");
    }

    @Test
    void readsSpringAiResponseContentAndUsage() {
        AtomicReference<Prompt> promptRef = new AtomicReference<>();
        OpenAiCompatibleChatClient client = client((config, options) -> prompt -> {
            promptRef.set(prompt);
            assertThat(options.getBaseUrl()).isEqualTo("https://api.example.com");
            assertThat(options.getApiKey()).isEqualTo("sk-secret");
            assertThat(options.getModel()).isEqualTo("model-a");
            assertThat(options.getTemperature()).isEqualTo(0.2);
            assertThat(options.getResponseFormat().getType().name()).isEqualTo("JSON_OBJECT");
            return response("{\"ok\":true}", usage(12, 8, 20));
        });

        AiChatCompletionResult result = client.completeJson(
            new AiAgentRuntimeConfig(true, "https://api.example.com/", "sk-secret", "model-a", 30, 300, 300),
            "system prompt",
            "user prompt"
        );

        assertThat(result.content()).isEqualTo("{\"ok\":true}");
        assertThat(result.promptTokens()).isEqualTo(12);
        assertThat(result.completionTokens()).isEqualTo(8);
        assertThat(result.totalTokens()).isEqualTo(20);
        assertThat(result.requestBodyJson()).contains("\"baseUrl\":\"https://api.example.com\"");
        assertThat(result.requestBodyJson()).contains("\"model\":\"model-a\"");
        assertThat(result.requestBodyJson()).contains("system prompt");
        assertThat(result.requestBodyJson()).doesNotContain("sk-secret");
        assertThat(result.rawResponseBody()).contains("\"content\":\"{\\\"ok\\\":true}\"");
        assertThat(result.rawResponseBody()).contains("\"totalTokens\":20");
        assertThat(promptRef.get().getSystemMessage().getText()).isEqualTo("system prompt");
        assertThat(promptRef.get().getUserMessage().getText()).isEqualTo("user prompt");
    }

    @Test
    void keepsTokenMetricsNullWhenUsageIsMissing() {
        OpenAiCompatibleChatClient client = client((config, options) -> responseModel("{\"ok\":true}", null));

        AiChatCompletionResult result = client.completeJson(
            new AiAgentRuntimeConfig(true, "https://api.example.com", "sk-secret", "model-a", 30, 300, 300),
            "system",
            "user"
        );

        assertThat(result.promptTokens()).isNull();
        assertThat(result.completionTokens()).isNull();
        assertThat(result.totalTokens()).isNull();
        assertThat(result.rawResponseBody()).contains("\"results\"");
    }

    @Test
    void rejectsEmptyResponseWithSerializedSpringAiResponse() {
        OpenAiCompatibleChatClient client = client((config, options) -> responseModel("", usage(1, 0, 1)));

        assertThatThrownBy(() -> client.completeJson(new AiAgentRuntimeConfig(true, "https://api.example.com", "sk-secret", "model-a", 30, 300, 300), "system", "user"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ai agent returned empty response")
            .hasMessageContaining("POST https://api.example.com/chat/completions")
            .hasMessageContaining("response=");
    }

    @Test
    void buildsHttpTimeoutFromRuntimeConfig() {
        Timeout timeout = OpenAiCompatibleChatClient.httpTimeout(
            new AiAgentRuntimeConfig(true, "https://api.example.com", "sk", "model-a", 15, 600, 480)
        );

        assertThat(timeout.connect()).isEqualTo(Duration.ofSeconds(15));
        assertThat(timeout.read()).isEqualTo(Duration.ofSeconds(600));
        assertThat(timeout.request()).isEqualTo(Duration.ofSeconds(480));
    }

    private OpenAiCompatibleChatClient client(OpenAiCompatibleChatClient.ChatModelFactory factory) {
        return new OpenAiCompatibleChatClient(objectMapper, factory);
    }

    private ChatModel responseModel(String content, Usage usage) {
        return prompt -> response(content, usage);
    }

    private ChatResponse response(String content, Usage usage) {
        return new ChatResponse(
            List.of(new Generation(new AssistantMessage(content))),
            ChatResponseMetadata.builder()
                .id("chatcmpl-test")
                .model("model-a")
                .usage(usage)
                .build()
        );
    }

    private Usage usage(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
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
}
