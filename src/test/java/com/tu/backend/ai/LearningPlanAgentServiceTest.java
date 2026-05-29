package com.tu.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.ai.dto.GenerateLearningPlanRequest;
import com.tu.backend.ai.entity.AiAgentRunLogEntity;
import com.tu.backend.common.BusinessException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LearningPlanAgentServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiAgentRuntimeConfig runtimeConfig = new AiAgentRuntimeConfig(true, "https://api.example.com", "sk-secret", "model-a");

    @Test
    void generatesSanitizedLearningPlan() {
        LearningPlanAgentService service = service((config, system, user) -> completion("""
            {
              "title": "Java 学习计划",
              "totalEstimatedHours": 99,
              "items": [
                {
                  "title": "基础",
                  "estimatedHours": 10,
                  "children": [
                    {"title": "语法", "estimatedHours": 2.5, "resource": "https://example.com/java"},
                    {"title": "集合", "estimatedHours": 3}
                  ]
                }
              ]
            }
            """));

        var response = service.generate(new GenerateLearningPlanRequest("Java", 12.0, 1.5, null));

        assertThat(response.title()).isEqualTo("Java 学习计划");
        assertThat(response.totalEstimatedHours()).isEqualTo(5.5);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().estimatedHours()).isNull();
        assertThat(response.items().getFirst().children()).hasSize(2);
    }

    @Test
    void rejectsInvalidJson() {
        LearningPlanAgentService service = service((config, system, user) -> completion("{bad"));

        assertThatThrownBy(() -> service.generate(new GenerateLearningPlanRequest("Java", null, null, null)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ai agent returned invalid learning plan json")
            .hasMessageContaining("JsonParseException")
            .hasMessageContaining("rawResponse={bad");
    }

    @Test
    void rejectsNegativeHours() {
        LearningPlanAgentService service = service((config, system, user) -> completion("""
            {"title":"x","items":[{"title":"step","estimatedHours":-1}]}
            """));

        assertThatThrownBy(() -> service.generate(new GenerateLearningPlanRequest("Java", null, null, null)))
            .isInstanceOf(BusinessException.class)
            .hasMessage("ai agent returned invalid estimated hours");
    }

    @Test
    void asksAgentToDefaultToChineseOutput() {
        AtomicReference<String> systemPrompt = new AtomicReference<>();
        LearningPlanAgentService service = service((config, system, user) -> {
            systemPrompt.set(system);
            return completion("""
                {"title":"Java 学习计划","items":[{"title":"语法","estimatedHours":1}]}
                """);
        });

        service.generate(new GenerateLearningPlanRequest("Java", null, null, null));

        assertThat(systemPrompt.get()).contains(AiAgentPrompts.DEFAULT_CHINESE_OUTPUT_CONSTRAINT);
    }

    @Test
    void recordsSuccessfulGenerationWithPromptCompletionAndOutput() {
        AiAgentRunLogEntity runLog = new AiAgentRunLogEntity();
        runLog.setId("run-test");
        AiAgentRunLogService runLogService = mock(AiAgentRunLogService.class);
        when(runLogService.start(anyString(), any(AiAgentRuntimeConfig.class), anyString(), anyString())).thenReturn(runLog);
        when(runLogService.markSuccess(anyString(), any(AiChatCompletionResult.class), anyString())).thenReturn(runLog);
        AiChatCompletionResult completion = completion("""
            {"title":"Java 学习计划","items":[{"title":"语法","estimatedHours":1.5}]}
            """);
        LearningPlanAgentService service = new LearningPlanAgentService(
            (config, system, user) -> completion,
            () -> runtimeConfig,
            runLogService,
            objectMapper
        );

        service.generate(new GenerateLearningPlanRequest("Java", null, null, null));

        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(runLogService).start(
            eq(AiAgentRunLogService.TASK_LEARNING_PLAN),
            eq(runtimeConfig),
            systemPrompt.capture(),
            userPrompt.capture()
        );
        assertThat(systemPrompt.getValue()).contains("Return only valid JSON");
        assertThat(userPrompt.getValue()).contains("Learning goal: Java");

        ArgumentCaptor<String> output = ArgumentCaptor.forClass(String.class);
        verify(runLogService).markSuccess(eq("run-test"), eq(completion), output.capture());
        assertThat(output.getValue()).contains("\"title\":\"Java 学习计划\"");
        assertThat(output.getValue()).contains("\"totalEstimatedHours\":1.5");
    }

    @Test
    void recordsFailedGenerationWithCompletionAndError() {
        AiAgentRunLogEntity runLog = new AiAgentRunLogEntity();
        runLog.setId("run-test");
        AiAgentRunLogService runLogService = mock(AiAgentRunLogService.class);
        when(runLogService.start(anyString(), any(AiAgentRuntimeConfig.class), anyString(), anyString())).thenReturn(runLog);
        when(runLogService.markFailed(anyString(), any(), any(Throwable.class))).thenReturn(runLog);
        AiChatCompletionResult completion = completion("{bad");
        LearningPlanAgentService service = new LearningPlanAgentService(
            (config, system, user) -> completion,
            () -> runtimeConfig,
            runLogService,
            objectMapper
        );

        assertThatThrownBy(() -> service.generate(new GenerateLearningPlanRequest("Java", null, null, null)))
            .isInstanceOf(BusinessException.class);

        ArgumentCaptor<Throwable> error = ArgumentCaptor.forClass(Throwable.class);
        verify(runLogService).markFailed(eq("run-test"), eq(completion), error.capture());
        assertThat(error.getValue()).hasMessageContaining("ai agent returned invalid learning plan json");
    }

    @Test
    void doesNotMaskGenerationFailureWhenFailureLogPersistenceFails() {
        AiAgentRunLogEntity runLog = new AiAgentRunLogEntity();
        runLog.setId("run-test");
        AiAgentRunLogService runLogService = mock(AiAgentRunLogService.class);
        when(runLogService.start(anyString(), any(AiAgentRuntimeConfig.class), anyString(), anyString())).thenReturn(runLog);
        when(runLogService.markFailed(anyString(), any(), any(Throwable.class)))
            .thenThrow(new RuntimeException("log persistence failed"));
        LearningPlanAgentService service = new LearningPlanAgentService(
            (config, system, user) -> completion("{bad"),
            () -> runtimeConfig,
            runLogService,
            objectMapper
        );

        assertThatThrownBy(() -> service.generate(new GenerateLearningPlanRequest("Java", null, null, null)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ai agent returned invalid learning plan json");
    }

    @Test
    void doesNotBlockGenerationWhenRunLogStartFails() {
        AiAgentRunLogService runLogService = mock(AiAgentRunLogService.class);
        when(runLogService.start(anyString(), any(AiAgentRuntimeConfig.class), anyString(), anyString()))
            .thenThrow(new RuntimeException("log start failed"));
        LearningPlanAgentService service = new LearningPlanAgentService(
            (config, system, user) -> completion("""
                {"title":"Java 学习计划","items":[{"title":"语法","estimatedHours":1}]}
                """),
            () -> runtimeConfig,
            runLogService,
            objectMapper
        );

        var response = service.generate(new GenerateLearningPlanRequest("Java", null, null, null));

        assertThat(response.title()).isEqualTo("Java 学习计划");
        verify(runLogService, never()).markSuccess(anyString(), any(AiChatCompletionResult.class), anyString());
    }

    private LearningPlanAgentService service(AiChatClient chatClient) {
        AiAgentRunLogEntity runLog = new AiAgentRunLogEntity();
        runLog.setId("run-test");
        AiAgentRunLogService runLogService = mock(AiAgentRunLogService.class);
        when(runLogService.start(anyString(), any(AiAgentRuntimeConfig.class), anyString(), anyString())).thenReturn(runLog);
        when(runLogService.markSuccess(anyString(), any(AiChatCompletionResult.class), anyString())).thenReturn(runLog);
        when(runLogService.markFailed(anyString(), any(), any(Throwable.class))).thenReturn(runLog);
        return new LearningPlanAgentService(chatClient, () -> runtimeConfig, runLogService, objectMapper);
    }

    private AiChatCompletionResult completion(String content) {
        return new AiChatCompletionResult(
            content,
            "{\"model\":\"model-a\"}",
            "{\"choices\":[{\"message\":{\"content\":\"" + content.replace("\"", "\\\"") + "\"}}]}",
            12L,
            null,
            null,
            null
        );
    }
}
