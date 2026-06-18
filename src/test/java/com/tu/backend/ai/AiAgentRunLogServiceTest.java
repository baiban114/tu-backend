package com.tu.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tu.backend.ai.entity.AiAgentRunLogEntity;
import com.tu.backend.ai.repository.AiAgentRunLogRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class AiAgentRunLogServiceTest {

    @Test
    void marksSuccessfulRunWithPromptsResponseOutputAndUsage() {
        TestContext context = new TestContext();
        AiAgentRunLogService service = context.service;
        AiAgentRunLogEntity started = service.start(
            AiAgentRunLogService.TASK_LEARNING_PLAN,
            new AiAgentRuntimeConfig(true, "https://api.example.com", "sk-secret", "model-a", 30, 300, 300),
            "system prompt",
            "user prompt"
        );

        service.markSuccess(started.getId(), new AiChatCompletionResult(
            "{\"ok\":true}",
            "{\"model\":\"model-a\"}",
            "{\"choices\":[],\"usage\":{\"prompt_tokens\":11,\"completion_tokens\":7,\"total_tokens\":18}}",
            123L,
            11,
            7,
            18
        ), "{\"title\":\"输出\"}");

        AiAgentRunLogEntity saved = context.logs.get(started.getId());
        assertThat(saved.getStatus()).isEqualTo(AiAgentRunLogService.STATUS_SUCCESS);
        assertThat(saved.getSystemPrompt()).isEqualTo("system prompt");
        assertThat(saved.getUserPrompt()).isEqualTo("user prompt");
        assertThat(saved.getRequestBodyJson()).contains("model-a");
        assertThat(saved.getRawResponseBody()).contains("usage");
        assertThat(saved.getOutputText()).contains("输出");
        assertThat(saved.getPromptTokens()).isEqualTo(11);
        assertThat(saved.getCompletionTokens()).isEqualTo(7);
        assertThat(saved.getTotalTokens()).isEqualTo(18);
        assertThat(saved.getDurationMs()).isEqualTo(123L);
    }

    @Test
    void keepsTokenMetricsNullWhenProviderOmitsUsage() {
        TestContext context = new TestContext();
        AiAgentRunLogEntity started = context.service.start(
            AiAgentRunLogService.TASK_LEARNING_PLAN,
            new AiAgentRuntimeConfig(true, "https://api.example.com", "sk-secret", "model-a", 30, 300, 300),
            "system",
            "user"
        );

        context.service.markSuccess(started.getId(), new AiChatCompletionResult(
            "{}",
            "{}",
            "{}",
            1L,
            null,
            null,
            null
        ), "{}");

        AiAgentRunLogEntity saved = context.logs.get(started.getId());
        assertThat(saved.getPromptTokens()).isNull();
        assertThat(saved.getCompletionTokens()).isNull();
        assertThat(saved.getTotalTokens()).isNull();
    }

    @Test
    void marksFailedRunWithErrorAndPartialCompletionDetails() {
        TestContext context = new TestContext();
        AiAgentRunLogEntity started = context.service.start(
            AiAgentRunLogService.TASK_LEARNING_PLAN,
            new AiAgentRuntimeConfig(true, "https://api.example.com", "sk-secret", "model-a", 30, 300, 300),
            "system",
            "user"
        );

        context.service.markFailed(started.getId(), new AiChatCompletionResult(
            "{bad",
            "{\"model\":\"model-a\"}",
            "{\"choices\":[{\"message\":{\"content\":\"{bad\"}}]}",
            9L,
            3,
            2,
            5
        ), new RuntimeException("invalid json"));

        AiAgentRunLogEntity saved = context.logs.get(started.getId());
        assertThat(saved.getStatus()).isEqualTo(AiAgentRunLogService.STATUS_FAILED);
        assertThat(saved.getRequestBodyJson()).contains("model-a");
        assertThat(saved.getRawResponseBody()).contains("choices");
        assertThat(saved.getOutputText()).isEqualTo("{bad");
        assertThat(saved.getErrorMessage()).isEqualTo("invalid json");
        assertThat(saved.getTotalTokens()).isEqualTo(5);
    }

    @Test
    void listsRunLogsFromNewestPage() {
        AiAgentRunLogRepository repository = mock(AiAgentRunLogRepository.class);
        AiAgentRunLogEntity newer = entity("run-newer");
        AiAgentRunLogEntity older = entity("run-older");
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(newer, older)));
        AiAgentRunLogService service = new AiAgentRunLogService(repository);

        var page = service.list("", "", 0, 50);

        assertThat(page.items()).extracting("id").containsExactly("run-newer", "run-older");
        assertThat(page.page()).isZero();
        assertThat(page.pageSize()).isEqualTo(50);
    }

    private static AiAgentRunLogEntity entity(String id) {
        AiAgentRunLogEntity entity = new AiAgentRunLogEntity();
        entity.setId(id);
        entity.setTaskType(AiAgentRunLogService.TASK_LEARNING_PLAN);
        entity.setStatus(AiAgentRunLogService.STATUS_SUCCESS);
        entity.setStartedAt(java.time.LocalDateTime.now());
        return entity;
    }

    private static final class TestContext {
        private final Map<String, AiAgentRunLogEntity> logs = new HashMap<>();
        private final AiAgentRunLogRepository repository = mock(AiAgentRunLogRepository.class);
        private final AiAgentRunLogService service = new AiAgentRunLogService(repository);

        TestContext() {
            when(repository.save(any(AiAgentRunLogEntity.class))).thenAnswer(invocation -> {
                AiAgentRunLogEntity entity = invocation.getArgument(0);
                logs.put(entity.getId(), entity);
                return entity;
            });
            when(repository.findById(any())).thenAnswer(invocation -> {
                String id = invocation.getArgument(0);
                return Optional.ofNullable(logs.get(id));
            });
        }
    }
}
