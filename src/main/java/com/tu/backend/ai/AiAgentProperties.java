package com.tu.backend.ai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.agent")
public class AiAgentProperties {

    private final ToolLoop toolLoop = new ToolLoop();
    private final WebSearch webSearch = new WebSearch();
    private final HttpClient httpClient = new HttpClient();

    public ToolLoop getToolLoop() {
        return toolLoop;
    }

    public WebSearch getWebSearch() {
        return webSearch;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public static class ToolLoop {

        private boolean enabled = true;
        private int maxToolRounds = 8;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxToolRounds() {
            return maxToolRounds;
        }

        public void setMaxToolRounds(int maxToolRounds) {
            this.maxToolRounds = maxToolRounds;
        }
    }

    public static class WebSearch {

        private String tavilyApiKey = "";
        private int maxResults = 5;

        public String getTavilyApiKey() {
            return tavilyApiKey;
        }

        public void setTavilyApiKey(String tavilyApiKey) {
            this.tavilyApiKey = tavilyApiKey == null ? "" : tavilyApiKey;
        }

        public int getMaxResults() {
            return maxResults;
        }

        public void setMaxResults(int maxResults) {
            this.maxResults = maxResults;
        }
    }

    public static class HttpClient {

        private Duration connectTimeout = Duration.ofSeconds(30);
        private Duration readTimeout = Duration.ofMinutes(5);
        private Duration requestTimeout = Duration.ofMinutes(5);

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }

        public Duration getRequestTimeout() {
            return requestTimeout;
        }

        public void setRequestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
        }
    }
}
