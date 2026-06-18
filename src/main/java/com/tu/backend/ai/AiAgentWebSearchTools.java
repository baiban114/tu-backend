package com.tu.backend.ai;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class AiAgentWebSearchTools {

    private final WebSearchClient webSearchClient;

    public AiAgentWebSearchTools(WebSearchClient webSearchClient) {
        this.webSearchClient = webSearchClient;
    }

    @Tool(description = """
        Search the public web and return matching pages (title, URL, snippet).
        Use when you need information that other tools cannot provide.
        """)
    public String searchWeb(
        @ToolParam(description = "Search query string") String query
    ) {
        return webSearchClient.search(query);
    }
}
