package com.tu.backend.ai;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiCompatibleChatClientTest {

    private RestClient.Builder restClientBuilder;
    private MockRestServiceServer server;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void rejectsDisabledAgent() {
        OpenAiCompatibleChatClient client = new OpenAiCompatibleChatClient(
            () -> new AiAgentRuntimeConfig(false, "", "", ""),
            restClientBuilder,
            objectMapper
        );

        assertThatThrownBy(() -> client.completeJson("system", "user"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("ai agent disabled");
    }

    @Test
    void rejectsIncompleteConfiguration() {
        OpenAiCompatibleChatClient client = new OpenAiCompatibleChatClient(
            () -> new AiAgentRuntimeConfig(true, "https://api.example.com/v1", "", "model"),
            restClientBuilder,
            objectMapper
        );

        assertThatThrownBy(() -> client.completeJson("system", "user"))
            .isInstanceOf(BusinessException.class)
            .hasMessage("ai agent configuration incomplete");
    }

    @Test
    void includesHttpFailureDetails() {
        server.expect(once(), requestTo("https://api.example.com/v1/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withUnauthorizedRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"message\":\"bad api key\",\"type\":\"auth_error\"}}"));

        OpenAiCompatibleChatClient client = new OpenAiCompatibleChatClient(
            () -> new AiAgentRuntimeConfig(true, "https://api.example.com/v1", "sk-secret", "model-a"),
            restClientBuilder,
            objectMapper
        );

        assertThatThrownBy(() -> client.completeJson("system", "user"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ai agent request failed")
            .hasMessageContaining("POST https://api.example.com/v1/chat/completions")
            .hasMessageContaining("model=model-a")
            .hasMessageContaining("httpStatus=401")
            .hasMessageContaining("bad api key")
            .hasMessageNotContaining("sk-secret");
    }

    @Test
    void wrapsUnexpectedRequestFailuresWithDetails() {
        ClientHttpRequestFactory failingFactory = (uri, method) -> {
            throw new IllegalArgumentException("synthetic request factory failure");
        };
        OpenAiCompatibleChatClient client = new OpenAiCompatibleChatClient(
            () -> new AiAgentRuntimeConfig(true, "https://api.example.com", "sk-secret", "model-a"),
            RestClient.builder().requestFactory(failingFactory),
            objectMapper
        );

        assertThatThrownBy(() -> client.completeJson("system", "user"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ai agent request failed")
            .hasMessageContaining("POST https://api.example.com/chat/completions")
            .hasMessageContaining("model=model-a")
            .hasMessageContaining("java.lang.IllegalArgumentException")
            .hasMessageContaining("synthetic request factory failure")
            .hasMessageNotContaining("sk-secret");
    }

    @Test
    void readsResponseAsStringAndParsesContent() {
        server.expect(once(), requestTo("https://api.example.com/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(
                "{\"choices\":[{\"message\":{\"content\":\"{\\\"ok\\\":true}\"}}]}",
                MediaType.APPLICATION_JSON
            ));

        OpenAiCompatibleChatClient client = new OpenAiCompatibleChatClient(
            () -> new AiAgentRuntimeConfig(true, "https://api.example.com", "sk-secret", "model-a"),
            restClientBuilder,
            objectMapper
        );

        assertThat(client.completeJson("system", "user")).isEqualTo("{\"ok\":true}");
    }
}
