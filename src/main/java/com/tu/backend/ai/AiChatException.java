package com.tu.backend.ai;

import com.tu.backend.common.BusinessException;

public class AiChatException extends BusinessException {

    private final String requestBodyJson;
    private final String rawResponseBody;
    private final Long durationMs;
    private final Integer promptTokens;
    private final Integer completionTokens;
    private final Integer totalTokens;

    public AiChatException(
        int code,
        String message,
        String requestBodyJson,
        String rawResponseBody,
        Long durationMs,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens
    ) {
        super(code, message);
        this.requestBodyJson = requestBodyJson;
        this.rawResponseBody = rawResponseBody;
        this.durationMs = durationMs;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }

    public String getRequestBodyJson() {
        return requestBodyJson;
    }

    public String getRawResponseBody() {
        return rawResponseBody;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }
}
