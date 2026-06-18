package com.tu.backend.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class WebSearchClient {

    private static final Logger log = LoggerFactory.getLogger(WebSearchClient.class);

    private final AiAgentProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;

    public WebSearchClient(
        AiAgentProperties properties,
        ObjectMapper objectMapper,
        RestClient.Builder restClientBuilder
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClientBuilder = restClientBuilder;
    }

    public String search(String query) {
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.isBlank()) {
            return toJson(Map.of("error", "query is required"));
        }
        AiAgentProperties.WebSearch webSearch = properties.getWebSearch();
        if (webSearch.getTavilyApiKey().isBlank()) {
            return toJson(Map.of(
                "error", "web search not configured",
                "hint", "Server administrator must set TAVILY_API_KEY"
            ));
        }
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("api_key", webSearch.getTavilyApiKey());
            body.put("query", trimmed);
            body.put("max_results", Math.max(1, Math.min(webSearch.getMaxResults(), 10)));
            body.put("search_depth", "basic");
            String responseBody = restClientBuilder.build()
                .post()
                .uri("https://api.tavily.com/search")
                .body(body)
                .retrieve()
                .body(String.class);
            return normalizeTavilyResponse(responseBody);
        } catch (RestClientException ex) {
            log.warn("web search request failed: {}", ex.getMessage());
            return toJson(Map.of("error", "web search request failed"));
        }
    }

    private String normalizeTavilyResponse(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return toJson(Map.of("error", "web search returned empty response"));
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            List<Map<String, Object>> results = new ArrayList<>();
            JsonNode resultNodes = root.path("results");
            if (resultNodes.isArray()) {
                for (JsonNode node : resultNodes) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("title", node.path("title").asText(""));
                    item.put("url", node.path("url").asText(""));
                    item.put("content", truncate(node.path("content").asText(""), 500));
                    results.add(item);
                }
            }
            return toJson(Map.of(
                "query", root.path("query").asText(""),
                "answer", truncate(root.path("answer").asText(""), 800),
                "results", results
            ));
        } catch (Exception ex) {
            log.warn("failed to parse web search response: {}", ex.getMessage());
            return toJson(Map.of("error", "web search response parse failed"));
        }
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return "{\"error\":\"serialization failed\"}";
        }
    }
}
