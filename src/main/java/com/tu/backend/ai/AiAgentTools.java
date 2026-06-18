package com.tu.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.common.BusinessException;
import com.tu.backend.rag.RagClient;
import com.tu.backend.rag.dto.RagQueryRequest;
import com.tu.backend.rag.dto.RagQueryResponse;
import com.tu.backend.rag.dto.RagSourceDto;
import com.tu.backend.search.SearchService;
import com.tu.backend.search.dto.SearchHitDto;
import com.tu.backend.search.dto.SearchResponseDto;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class AiAgentTools {

    private final SearchService searchService;
    private final RagClient ragClient;
    private final ObjectMapper objectMapper;

    public AiAgentTools(
        SearchService searchService,
        RagClient ragClient,
        ObjectMapper objectMapper
    ) {
        this.searchService = searchService;
        this.ragClient = ragClient;
        this.objectMapper = objectMapper;
    }

    @Tool(description = """
        Keyword search across indexed knowledge-base pages (titles and block bodies).
        Use when you need to find existing notes or pages related to the learning topic.
        """)
    public String searchKnowledgeBasePages(
        @ToolParam(description = "Search keywords, at least 2 characters") String query,
        @ToolParam(description = "Maximum number of hits, default 5") Integer limit
    ) {
        int safeLimit = limit == null ? 5 : Math.clamp(limit, 1, 10);
        SearchResponseDto response = searchService.search(query, safeLimit);
        List<Map<String, Object>> hits = new ArrayList<>();
        for (SearchHitDto hit : response.hits()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("kbId", hit.kbId());
            item.put("kbName", hit.kbName());
            item.put("pageId", hit.pageId());
            item.put("pageTitle", hit.pageTitle());
            item.put("blockId", hit.blockId());
            item.put("snippet", hit.snippet());
            hits.add(item);
        }
        return toJson(Map.of(
            "enabled", response.enabled(),
            "message", response.message() == null ? "" : response.message(),
            "hits", hits
        ));
    }

    @Tool(description = """
        Semantic RAG retrieval within the current knowledge base.
        Requires kbId in the agent execution context. Returns relevant page blocks and a synthesized answer.
        """)
    public String queryKnowledgeBaseRag(
        @ToolParam(description = "Natural-language question about the learning topic") String query,
        @ToolParam(description = "Number of source chunks, default 5") Integer topK
    ) {
        AiAgentExecutionContext context = AiAgentExecutionContextHolder.get();
        String kbId = context == null ? "" : normalize(context.kbId());
        if (kbId.isBlank()) {
            return toJson(Map.of(
                "error", "knowledge base id not provided",
                "hint", "Use searchKnowledgeBasePages instead"
            ));
        }
        int safeTopK = topK == null ? 5 : Math.max(1, Math.min(topK, 10));
        try {
            RagQueryResponse response = ragClient.query(new RagQueryRequest(kbId, query, safeTopK));
            List<Map<String, Object>> sources = new ArrayList<>();
            if (response.sources() != null) {
                for (RagSourceDto source : response.sources()) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("pageId", source.pageId());
                    item.put("blockId", source.blockId());
                    item.put("title", source.title());
                    item.put("score", source.score());
                    item.put("content", truncate(source.content(), 500));
                    sources.add(item);
                }
            }
            return toJson(Map.of(
                "kbId", kbId,
                "answer", response.answer() == null ? "" : response.answer(),
                "sources", sources
            ));
        } catch (BusinessException ex) {
            return toJson(Map.of("error", ex.getMessage()));
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
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
