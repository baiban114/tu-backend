package com.tu.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.rag.RagClient;
import com.tu.backend.rag.dto.RagQueryRequest;
import com.tu.backend.rag.dto.RagQueryResponse;
import com.tu.backend.rag.dto.RagSourceDto;
import com.tu.backend.search.SearchService;
import com.tu.backend.search.dto.SearchHitDto;
import com.tu.backend.search.dto.SearchResponseDto;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiAgentToolsTest {

    @Mock
    private SearchService searchService;

    @Mock
    private RagClient ragClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void tearDown() {
        AiAgentExecutionContextHolder.clear();
    }

    @Test
    void searchKnowledgeBasePagesReturnsHits() {
        when(searchService.search(eq("java"), anyInt())).thenReturn(new SearchResponseDto(
            List.of(new SearchHitDto("kb-1", "Notes", "p-1", "Java Intro", "b-1", "richtext", "Intro", "snippet")),
            true,
            null
        ));
        AiAgentTools tools = new AiAgentTools(searchService, ragClient, objectMapper);

        String result = tools.searchKnowledgeBasePages("java", 5);

        assertThat(result).contains("\"pageTitle\":\"Java Intro\"");
        assertThat(result).contains("\"snippet\":\"snippet\"");
    }

    @Test
    void queryKnowledgeBaseRagRequiresKbId() {
        AiAgentTools tools = new AiAgentTools(searchService, ragClient, objectMapper);

        String result = tools.queryKnowledgeBaseRag("how to learn java", 5);

        assertThat(result).contains("knowledge base id not provided");
    }

    @Test
    void queryKnowledgeBaseRagUsesExecutionContext() {
        AiAgentExecutionContextHolder.set(new AiAgentExecutionContext("kb-1", "Java", false));
        when(ragClient.query(any(RagQueryRequest.class))).thenReturn(new RagQueryResponse(
            "Start with syntax basics.",
            List.of(new RagSourceDto("kb-1", "p-1", "b-1", "Syntax", "content", "richtext", 0.9))
        ));
        AiAgentTools tools = new AiAgentTools(searchService, ragClient, objectMapper);

        String result = tools.queryKnowledgeBaseRag("how to learn java", 5);

        assertThat(result).contains("\"answer\":\"Start with syntax basics.\"");
        assertThat(result).contains("\"title\":\"Syntax\"");
    }
}
