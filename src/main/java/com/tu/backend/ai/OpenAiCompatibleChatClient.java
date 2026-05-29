package com.tu.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.tu.backend.common.BusinessException;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpenAiCompatibleChatClient implements AiChatClient, AiAgentConnectionTester {

    private final ObjectMapper objectMapper;
    private final ChatModelFactory chatModelFactory;

    @Autowired
    public OpenAiCompatibleChatClient(ObjectMapper objectMapper) {
        this(objectMapper, OpenAiCompatibleChatClient::createOpenAiChatModel);
    }

    OpenAiCompatibleChatClient(
        ObjectMapper objectMapper,
        ChatModelFactory chatModelFactory
    ) {
        this.objectMapper = objectMapper;
        this.chatModelFactory = chatModelFactory;
    }

    @Override
    public AiChatCompletionResult completeJson(AiAgentRuntimeConfig config, String systemPrompt, String userPrompt) {
        ensureConfigured(config);
        String requestBodyJson = "";
        String rawResponseBody = null;
        long startedAt = System.nanoTime();
        try {
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                .baseUrl(stripTrailingSlash(config.baseUrl()))
                .apiKey(config.apiKey())
                .model(config.model())
                .temperature(0.2)
                .responseFormat(OpenAiChatModel.ResponseFormat.builder()
                    .type(OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT)
                    .build())
                .build();
            Map<String, Object> requestBody = Map.of(
                "baseUrl", stripTrailingSlash(config.baseUrl()),
                "model", config.model(),
                "temperature", 0.2,
                "response_format", Map.of("type", "json_object"),
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
                )
            );
            requestBodyJson = objectMapper.writeValueAsString(requestBody);
            ChatModel chatModel = chatModelFactory.create(config, options);
            ChatResponse response = ChatClient.create(chatModel)
                .prompt(new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)), options))
                .call()
                .chatResponse();
            rawResponseBody = serializeChatResponse(response);
            UsageSnapshot usage = usageFromResponse(response);
            String content = contentFromResponse(response);
            if (content.isBlank()) {
                throw new AiChatException(
                    50323,
                    "ai agent returned empty response: " + requestContext(config)
                        + "; response=" + abbreviate(rawResponseBody),
                    requestBodyJson,
                    rawResponseBody,
                    elapsedMillis(startedAt),
                    usage.promptTokens(),
                    usage.completionTokens(),
                    usage.totalTokens()
                );
            }
            return new AiChatCompletionResult(
                stripJsonFence(content),
                requestBodyJson,
                rawResponseBody,
                elapsedMillis(startedAt),
                usage.promptTokens(),
                usage.completionTokens(),
                usage.totalTokens()
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiChatException(
                50322,
                "ai agent request failed: " + requestContext(config) + "; " + exceptionDetail(ex),
                requestBodyJson,
                rawResponseBody,
                elapsedMillis(startedAt),
                null,
                null,
                null
            );
        }
    }

    private static ChatModel createOpenAiChatModel(AiAgentRuntimeConfig config, OpenAiChatOptions options) {
        var openAiClient = OpenAIOkHttpClient.builder()
            .baseUrl(stripTrailingSlash(config.baseUrl()))
            .apiKey(config.apiKey())
            .build();
        return OpenAiChatModel.builder()
            .openAiClient(openAiClient)
            .options(options)
            .build();
    }

    private String contentFromResponse(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        return nullToBlank(response.getResult().getOutput().getText());
    }

    private UsageSnapshot usageFromResponse(ChatResponse response) {
        Usage usage = response == null || response.getMetadata() == null
            ? null
            : response.getMetadata().getUsage();
        if (usage == null) {
            return new UsageSnapshot(null, null, null);
        }
        return new UsageSnapshot(
            usage.getPromptTokens(),
            usage.getCompletionTokens(),
            usage.getTotalTokens()
        );
    }

    private String serializeChatResponse(ChatResponse response) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("metadata", metadataMap(response == null ? null : response.getMetadata()));
            payload.put("results", response == null
                ? List.of()
                : response.getResults().stream().map(this::generationMap).toList());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return response == null ? "" : response.toString();
        }
    }

    private Map<String, Object> metadataMap(ChatResponseMetadata metadata) {
        if (metadata == null) {
            return Map.of();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", metadata.getId());
        payload.put("model", metadata.getModel());
        Usage usage = metadata.getUsage();
        if (usage != null) {
            Map<String, Object> usagePayload = new LinkedHashMap<>();
            usagePayload.put("promptTokens", usage.getPromptTokens());
            usagePayload.put("completionTokens", usage.getCompletionTokens());
            usagePayload.put("totalTokens", usage.getTotalTokens());
            payload.put("usage", usagePayload);
            if (usage.getNativeUsage() != null) {
                payload.put("nativeUsage", usage.getNativeUsage().toString());
            }
        }
        return payload;
    }

    private Map<String, Object> generationMap(Generation generation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        AssistantMessage output = generation == null ? null : generation.getOutput();
        payload.put("content", output == null ? "" : output.getText());
        payload.put("metadata", generation == null || generation.getMetadata() == null
            ? Map.of()
            : generation.getMetadata().toString());
        return payload;
    }

    private Long elapsedMillis(long startedAt) {
        return Math.max(0, Math.round((System.nanoTime() - startedAt) / 1_000_000.0));
    }

    private void ensureConfigured(AiAgentRuntimeConfig config) {
        if (!config.enabled()) {
            throw new BusinessException(50320, "ai agent disabled");
        }
        if (isBlank(config.baseUrl()) || isBlank(config.apiKey()) || isBlank(config.model())) {
            throw new BusinessException(50321, "ai agent configuration incomplete");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String requestContext(AiAgentRuntimeConfig config) {
        return "POST " + stripTrailingSlash(config.baseUrl()) + "/chat/completions; model=" + config.model();
    }

    private String exceptionDetail(Exception ex) {
        return "exception=" + ex.getClass().getName()
            + ": " + nullToBlank(ex.getMessage())
            + causeDetail(ex);
    }

    private String causeDetail(Throwable throwable) {
        List<String> causes = new ArrayList<>();
        Throwable cause = throwable.getCause();
        while (cause != null && causes.size() < 6) {
            causes.add(cause.getClass().getName() + ": " + nullToBlank(cause.getMessage()));
            cause = cause.getCause();
        }
        return causes.isEmpty() ? "" : "; causes=" + String.join(" <- ", causes);
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String abbreviate(String value) {
        String normalized = value == null ? "" : value.strip();
        int maxLength = 4000;
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...<truncated " + (normalized.length() - maxLength) + " chars>";
    }

    private String stripJsonFence(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("```")) {
            int firstBreak = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstBreak >= 0 && lastFence > firstBreak) {
                return trimmed.substring(firstBreak + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    interface ChatModelFactory {
        ChatModel create(AiAgentRuntimeConfig config, OpenAiChatOptions options);
    }

    private record UsageSnapshot(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
    }
}
