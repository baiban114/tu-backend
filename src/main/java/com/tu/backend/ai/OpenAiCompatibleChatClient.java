package com.tu.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.common.BusinessException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OpenAiCompatibleChatClient implements AiChatClient, AiAgentConnectionTester {

    private final AiAgentRuntimeConfigResolver configResolver;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleChatClient(
        AiAgentRuntimeConfigResolver configResolver,
        RestClient.Builder restClientBuilder,
        ObjectMapper objectMapper
    ) {
        this.configResolver = configResolver;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
    }

    @Override
    public String completeJson(String systemPrompt, String userPrompt) {
        AiAgentRuntimeConfig config = configResolver.runtimeConfig();
        ensureConfigured(config);
        return completeJson(config, systemPrompt, userPrompt);
    }

    public String completeJson(AiAgentRuntimeConfig config, String systemPrompt, String userPrompt) {
        ensureConfigured(config);
        try {
            RestClient client = restClientBuilder.baseUrl(stripTrailingSlash(config.baseUrl())).build();
            String responseBody = client.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + config.apiKey())
                .body(Map.of(
                    "model", config.model(),
                    "temperature", 0.2,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                    )
                ))
                .retrieve()
                .body(String.class);
            JsonNode response = parseResponse(config, responseBody);
            JsonNode content = response == null
                ? null
                : response.at("/choices/0/message/content");
            if (content == null || content.isMissingNode() || content.asText("").isBlank()) {
                throw new BusinessException(
                    50323,
                    "ai agent returned empty response: " + requestContext(config)
                        + "; response=" + abbreviate(response == null ? "null" : response.toString())
                );
            }
            return stripJsonFence(content.asText());
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new BusinessException(50322, "ai agent request failed: " + requestContext(config) + "; " + exceptionDetail(ex));
        } catch (Exception ex) {
            throw new BusinessException(50322, "ai agent request failed: " + requestContext(config) + "; " + exceptionDetail(ex));
        }
    }

    private JsonNode parseResponse(AiAgentRuntimeConfig config, String responseBody) {
        if (isBlank(responseBody)) {
            return null;
        }
        try {
            return objectMapper.readTree(responseBody);
        } catch (Exception ex) {
            throw new BusinessException(
                50323,
                "ai agent returned invalid response json: " + requestContext(config)
                    + "; exception=" + ex.getClass().getName()
                    + ": " + nullToBlank(ex.getMessage())
                    + "; responseBody=" + abbreviate(responseBody)
            );
        }
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

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String requestContext(AiAgentRuntimeConfig config) {
        return "POST " + stripTrailingSlash(config.baseUrl()) + "/chat/completions; model=" + config.model();
    }

    private String exceptionDetail(RestClientException ex) {
        if (ex instanceof RestClientResponseException responseException) {
            String body = responseException.getResponseBodyAsString();
            return "httpStatus=" + responseException.getStatusCode().value()
                + " " + responseException.getStatusText()
                + "; exception=" + responseException.getClass().getName()
                + ": " + nullToBlank(responseException.getMessage())
                + "; responseBody=" + abbreviate(body.isBlank() ? "<empty>" : body);
        }
        return "exception=" + ex.getClass().getName()
            + ": " + nullToBlank(ex.getMessage())
            + causeDetail(ex);
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
}
