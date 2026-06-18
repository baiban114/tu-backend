package com.tu.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiAgentWebSearchToolsTest {

    @Mock
    private WebSearchClient webSearchClient;

    @Test
    void searchWebDelegatesToClient() {
        when(webSearchClient.search("spring boot 4")).thenReturn("{\"results\":[]}");
        AiAgentWebSearchTools tools = new AiAgentWebSearchTools(webSearchClient);

        assertThat(tools.searchWeb("spring boot 4")).isEqualTo("{\"results\":[]}");
    }
}
