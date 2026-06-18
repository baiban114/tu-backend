package com.tu.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.core.Timeout;
import com.tu.backend.common.BusinessException;
import java.time.Duration;
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
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpenAiCompatibleChatClient implements AiChatClient, AiAgentConnectionTester, AiAgentToolLoopClient {

    private final ObjectMapper objectMapper;
    private final ChatModelFactory chatModelFactory;
    private final ToolCallingManager toolCallingManager;
    private final AiAgentProperties aiAgentProperties;

    @Autowired
    public OpenAiCompatibleChatClient(
        ObjectMapper objectMapper,
        ToolCallingManager toolCallingManager,
        AiAgentProperties aiAgentProperties
    ) {
        this(objectMapper, (config, options) -> createOpenAiChatModel(config, options), toolCallingManager, aiAgentProperties);
    }

    OpenAiCompatibleChatClient(
        ObjectMapper objectMapper,
        ChatModelFactory chatModelFactory
    ) {
        this(objectMapper, chatModelFactory, ToolCallingManager.builder().build(), new AiAgentProperties());
    }

    OpenAiCompatibleChatClient(
        ObjectMapper objectMapper,
        ChatModelFactory chatModelFactory,
        ToolCallingManager toolCallingManager,
        AiAgentProperties aiAgentProperties
    ) {
        this.objectMapper = objectMapper;
        this.chatModelFactory = chatModelFactory;
        this.toolCallingManager = toolCallingManager;
        this.aiAgentProperties = aiAgentProperties;
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
                .timeout(requestTimeoutDuration(config))
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
                AiAgentJsonContent.extract(content),
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

    @Override
    public AiAgentToolLoopResult runToolLoop(
        AiAgentRuntimeConfig config,
        String systemPrompt,
        String userPrompt,
        AiAgentExecutionContext executionContext,
        AiAgentProgressListener progressListener,
        Object... toolProviders
    ) {
        ensureConfigured(config);
        AiAgentExecutionContextHolder.set(executionContext);
        String requestBodyJson = "";
        String rawResponseBody = null;
        long startedAt = System.nanoTime();
        int modelCallCount = 0;
        int toolRoundCount = 0;
        try {
            List<ToolCallback> toolCallbacks = AiAgentToolCallbackSupport.fromProviders(toolProviders);
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                .baseUrl(stripTrailingSlash(config.baseUrl()))
                .apiKey(config.apiKey())
                .model(config.model())
                .temperature(0.2)
                .timeout(requestTimeoutDuration(config))
                .toolCallbacks(toolCallbacks)
                .build();
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("baseUrl", stripTrailingSlash(config.baseUrl()));
            requestBody.put("model", config.model());
            requestBody.put("temperature", 0.2);
            requestBody.put("toolLoop", true);
            requestBody.put("maxToolRounds", aiAgentProperties.getToolLoop().getMaxToolRounds());
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
            ));
            requestBodyJson = objectMapper.writeValueAsString(requestBody);

            ChatModel chatModel = chatModelFactory.create(config, options);
            ChatOptions chatOptions = options;
            Prompt prompt = new Prompt(
                List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt)),
                chatOptions
            );

            ensureNotCancelled(progressListener);
            emitProgress(progressListener, AiAgentProgressEvent.of(
                AiAgentProgressEvent.phaseModelCall(),
                "正在调用模型（第 1 轮）",
                1,
                null,
                startedAt
            ));
            ChatResponse chatResponse = chatModel.call(prompt);
            modelCallCount++;

            int maxToolRounds = Math.max(1, aiAgentProperties.getToolLoop().getMaxToolRounds());
            while (chatResponse.hasToolCalls() && toolRoundCount < maxToolRounds) {
                toolRoundCount++;
                emitToolCallProgress(progressListener, chatResponse, startedAt);
                ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, chatResponse);
                emitToolDoneProgress(progressListener, chatResponse, startedAt);
                prompt = new Prompt(toolExecutionResult.conversationHistory(), chatOptions);
                ensureNotCancelled(progressListener);
                int nextRound = modelCallCount + 1;
                emitProgress(progressListener, AiAgentProgressEvent.of(
                    AiAgentProgressEvent.phaseModelCall(),
                    "正在调用模型（第 " + nextRound + " 轮）",
                    nextRound,
                    null,
                    startedAt
                ));
                chatResponse = chatModel.call(prompt);
                modelCallCount++;
            }

            rawResponseBody = serializeChatResponse(chatResponse);
            if (chatResponse.hasToolCalls()) {
                throw new BusinessException(
                    50325,
                    "ai agent exceeded max tool loop rounds: " + maxToolRounds
                );
            }

            UsageSnapshot usage = usageFromResponse(chatResponse);
            String content = AiAgentJsonContent.extract(contentFromResponse(chatResponse));
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
            return new AiAgentToolLoopResult(
                content,
                requestBodyJson,
                rawResponseBody,
                elapsedMillis(startedAt),
                usage.promptTokens(),
                usage.completionTokens(),
                usage.totalTokens(),
                modelCallCount,
                toolRoundCount
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiChatException(
                50322,
                "ai agent tool loop failed: " + requestContext(config) + "; " + exceptionDetail(ex),
                requestBodyJson,
                rawResponseBody,
                elapsedMillis(startedAt),
                null,
                null,
                null
            );
        } finally {
            AiAgentExecutionContextHolder.clear();
        }
    }

    private void ensureNotCancelled(AiAgentProgressListener progressListener) {
        if (progressListener != null && progressListener.isCancelled()) {
            throw new BusinessException(50326, "ai agent run cancelled");
        }
    }

    private void emitProgress(AiAgentProgressListener progressListener, AiAgentProgressEvent event) {
        if (progressListener != null) {
            progressListener.onEvent(event);
        }
    }

    private void emitToolCallProgress(
        AiAgentProgressListener progressListener,
        ChatResponse chatResponse,
        long startedAt
    ) {
        if (progressListener == null) {
            return;
        }
        for (String toolName : toolNamesFromResponse(chatResponse)) {
            emitProgress(progressListener, AiAgentProgressEvent.of(
                AiAgentProgressEvent.phaseToolCall(),
                AiAgentToolLabels.toolCallMessage(toolName),
                null,
                toolName,
                startedAt
            ));
        }
    }

    private void emitToolDoneProgress(
        AiAgentProgressListener progressListener,
        ChatResponse chatResponse,
        long startedAt
    ) {
        if (progressListener == null) {
            return;
        }
        for (String toolName : toolNamesFromResponse(chatResponse)) {
            emitProgress(progressListener, AiAgentProgressEvent.of(
                AiAgentProgressEvent.phaseToolDone(),
                AiAgentToolLabels.toolDoneMessage(toolName),
                null,
                toolName,
                startedAt
            ));
        }
    }

    private List<String> toolNamesFromResponse(ChatResponse chatResponse) {
        if (chatResponse == null || chatResponse.getResult() == null) {
            return List.of();
        }
        AssistantMessage output = chatResponse.getResult().getOutput();
        if (output == null || output.getToolCalls() == null) {
            return List.of();
        }
        return output.getToolCalls().stream()
            .map(AssistantMessage.ToolCall::name)
            .filter(name -> name != null && !name.isBlank())
            .distinct()
            .toList();
    }

    private static ChatModel createOpenAiChatModel(
        AiAgentRuntimeConfig config,
        OpenAiChatOptions options
    ) {
        Timeout timeout = httpTimeout(config);
        return OpenAiChatModel.builder()
            .options(options)
            .httpClientBuilderCustomizer(builder -> builder.timeout(timeout))
            .build();
    }

    static Timeout httpTimeout(AiAgentRuntimeConfig config) {
        Duration connect = Duration.ofSeconds(Math.max(1, config.connectTimeoutSeconds()));
        Duration read = Duration.ofSeconds(Math.max(1, config.readTimeoutSeconds()));
        Duration request = requestTimeoutDuration(config);
        return Timeout.builder()
            .connect(connect)
            .read(read)
            .write(read)
            .request(request)
            .build();
    }

    private static Duration requestTimeoutDuration(AiAgentRuntimeConfig config) {
        return Duration.ofSeconds(Math.max(1, config.requestTimeoutSeconds()));
    }

    static Timeout httpTimeout(AiAgentProperties.HttpClient httpClient) {
        Duration connect = positiveDuration(httpClient.getConnectTimeout(), Duration.ofSeconds(30));
        Duration read = positiveDuration(httpClient.getReadTimeout(), Duration.ofMinutes(5));
        Duration request = positiveDuration(httpClient.getRequestTimeout(), Duration.ofMinutes(5));
        return Timeout.builder()
            .connect(connect)
            .read(read)
            .write(read)
            .request(request)
            .build();
    }

    private static Duration positiveDuration(Duration value, Duration fallback) {
        if (value == null || value.isZero() || value.isNegative()) {
            return fallback;
        }
        return value;
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
        String detail = "exception=" + ex.getClass().getName()
            + ": " + nullToBlank(ex.getMessage())
            + causeDetail(ex);
        if (isTimeoutException(ex)) {
            detail += "; hint=模型响应超时，可在系统配置 > AI Agent 调大读超时/请求超时后重试";
        }
        return detail;
    }

    private boolean isTimeoutException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String name = current.getClass().getName();
            String message = nullToBlank(current.getMessage()).toLowerCase();
            if (name.contains("Timeout") || name.contains("InterruptedIOException") || message.contains("timeout")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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

    interface ChatModelFactory {
        ChatModel create(AiAgentRuntimeConfig config, OpenAiChatOptions options);
    }

    private record UsageSnapshot(Integer promptTokens, Integer completionTokens, Integer totalTokens) {
    }
}
