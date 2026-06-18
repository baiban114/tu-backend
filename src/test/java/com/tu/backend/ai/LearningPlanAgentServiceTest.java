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
    private final AiAgentRuntimeConfig runtimeConfig = new AiAgentRuntimeConfig(
        true, "https://api.example.com", "sk-secret", "model-a", 30, 300, 300
    );

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

        var response = service.generate(new GenerateLearningPlanRequest("Java", 12.0, 1.5, null, null, null));

        assertThat(response.title()).isEqualTo("Java 学习计划");
        assertThat(response.totalEstimatedHours()).isEqualTo(5.5);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().estimatedHours()).isNull();
        assertThat(response.items().getFirst().children()).hasSize(2);
    }

    @Test
    void rejectsInvalidJson() {
        LearningPlanAgentService service = service((config, system, user) -> completion("{bad"));

        assertThatThrownBy(() -> service.generate(new GenerateLearningPlanRequest("Java", null, null, null, null, null)))
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

        assertThatThrownBy(() -> service.generate(new GenerateLearningPlanRequest("Java", null, null, null, null, null)))
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

        service.generate(new GenerateLearningPlanRequest("Java", null, null, null, null, null));

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
            (config, system, user, context, listener, tools) -> {
                throw new IllegalStateException("tool loop should not be invoked");
            },
            mock(AiAgentTools.class),
            mock(AiAgentWebSearchTools.class),
            disabledToolLoopProperties(),
            () -> runtimeConfig,
            runLogService,
            objectMapper
        );

        service.generate(new GenerateLearningPlanRequest("Java", null, null, null, null, null));

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
            (config, system, user, context, listener, tools) -> {
                throw new IllegalStateException("tool loop should not be invoked");
            },
            mock(AiAgentTools.class),
            mock(AiAgentWebSearchTools.class),
            disabledToolLoopProperties(),
            () -> runtimeConfig,
            runLogService,
            objectMapper
        );

        assertThatThrownBy(() -> service.generate(new GenerateLearningPlanRequest("Java", null, null, null, null, null)))
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
            (config, system, user, context, listener, tools) -> {
                throw new IllegalStateException("tool loop should not be invoked");
            },
            mock(AiAgentTools.class),
            mock(AiAgentWebSearchTools.class),
            disabledToolLoopProperties(),
            () -> runtimeConfig,
            runLogService,
            objectMapper
        );

        assertThatThrownBy(() -> service.generate(new GenerateLearningPlanRequest("Java", null, null, null, null, null)))
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
            (config, system, user, context, listener, tools) -> {
                throw new IllegalStateException("tool loop should not be invoked");
            },
            mock(AiAgentTools.class),
            mock(AiAgentWebSearchTools.class),
            disabledToolLoopProperties(),
            () -> runtimeConfig,
            runLogService,
            objectMapper
        );

        var response = service.generate(new GenerateLearningPlanRequest("Java", null, null, null, null, null));

        assertThat(response.title()).isEqualTo("Java 学习计划");
        verify(runLogService, never()).markSuccess(anyString(), any(AiChatCompletionResult.class), anyString());
    }

    @Test
    void usesToolLoopWhenEnabled() {
        AtomicReference<AiAgentExecutionContext> contextRef = new AtomicReference<>();
        AiAgentToolLoopResult loopResult = new AiAgentToolLoopResult(
            """
            {"title":"Java 学习计划","items":[{"title":"语法","estimatedHours":1}]}
            """,
            "{\"toolLoop\":true}",
            "{\"results\":[]}",
            20L,
            10,
            5,
            15,
            2,
            1
        );
        LearningPlanAgentService service = service(
            (config, system, user) -> {
                throw new IllegalStateException("completeJson should not be invoked");
            },
            (config, system, user, context, listener, tools) -> {
                contextRef.set(context);
                return loopResult;
            },
            true
        );

        var response = service.generate(new GenerateLearningPlanRequest("Java", null, null, null, "kb-1", false));

        assertThat(response.title()).isEqualTo("Java 学习计划");
        assertThat(contextRef.get()).isNotNull();
        assertThat(contextRef.get().kbId()).isEqualTo("kb-1");
        assertThat(contextRef.get().topic()).isEqualTo("Java");
        assertThat(contextRef.get().enableWebSearch()).isFalse();
    }

    @Test
    void includesWebSearchToolWhenUserEnabled() {
        AtomicReference<Object[]> toolProvidersRef = new AtomicReference<>();
        AiAgentToolLoopResult loopResult = new AiAgentToolLoopResult(
            """
            {"title":"Java 学习计划","items":[{"title":"语法","estimatedHours":1}]}
            """,
            "{\"toolLoop\":true}",
            "{\"results\":[]}",
            20L,
            10,
            5,
            15,
            1,
            0
        );
        AiAgentTools knowledgeTools = mock(AiAgentTools.class);
        AiAgentWebSearchTools webSearchTools = mock(AiAgentWebSearchTools.class);
        LearningPlanAgentService service = new LearningPlanAgentService(
            (config, system, user) -> {
                throw new IllegalStateException("completeJson should not be invoked");
            },
            (config, system, user, context, listener, tools) -> {
                toolProvidersRef.set(tools);
                assertThat(context.enableWebSearch()).isTrue();
                return loopResult;
            },
            knowledgeTools,
            webSearchTools,
            enabledToolLoopProperties(),
            () -> runtimeConfig,
            mock(AiAgentRunLogService.class),
            objectMapper
        );

        service.generate(new GenerateLearningPlanRequest("Java", null, null, null, null, true));

        assertThat(toolProvidersRef.get()).containsExactly(knowledgeTools, webSearchTools);
    }

    @Test
    void guidesWebSearchForTutorialsInPromptsWhenEnabled() {
        AtomicReference<String> systemPrompt = new AtomicReference<>();
        AtomicReference<String> userPrompt = new AtomicReference<>();
        AiAgentToolLoopResult loopResult = new AiAgentToolLoopResult(
            """
            {"title":"Java 学习计划","items":[{"title":"语法","estimatedHours":1}]}
            """,
            "{\"toolLoop\":true}",
            "{\"results\":[]}",
            20L,
            10,
            5,
            15,
            1,
            0
        );
        LearningPlanAgentService service = new LearningPlanAgentService(
            (config, system, user) -> {
                throw new IllegalStateException("completeJson should not be invoked");
            },
            (config, system, user, context, listener, tools) -> {
                systemPrompt.set(system);
                userPrompt.set(user);
                return loopResult;
            },
            mock(AiAgentTools.class),
            mock(AiAgentWebSearchTools.class),
            enabledToolLoopProperties(),
            () -> runtimeConfig,
            mock(AiAgentRunLogService.class),
            objectMapper
        );

        service.generate(new GenerateLearningPlanRequest("Java", null, null, null, null, true));

        assertThat(systemPrompt.get()).contains("searchWeb");
        assertThat(systemPrompt.get()).contains("tutorials");
        assertThat(userPrompt.get()).contains("tutorials, documentation");
    }

    @Test
    void forbidsWebSearchInPromptsWhenDisabled() {
        AtomicReference<String> systemPrompt = new AtomicReference<>();
        AtomicReference<String> userPrompt = new AtomicReference<>();
        AiAgentToolLoopResult loopResult = new AiAgentToolLoopResult(
            """
            {"title":"Java 学习计划","items":[{"title":"语法","estimatedHours":1}]}
            """,
            "{\"toolLoop\":true}",
            "{\"results\":[]}",
            20L,
            10,
            5,
            15,
            1,
            0
        );
        LearningPlanAgentService service = new LearningPlanAgentService(
            (config, system, user) -> {
                throw new IllegalStateException("completeJson should not be invoked");
            },
            (config, system, user, context, listener, tools) -> {
                systemPrompt.set(system);
                userPrompt.set(user);
                return loopResult;
            },
            mock(AiAgentTools.class),
            mock(AiAgentWebSearchTools.class),
            enabledToolLoopProperties(),
            () -> runtimeConfig,
            mock(AiAgentRunLogService.class),
            objectMapper
        );

        service.generate(new GenerateLearningPlanRequest("Java", null, null, null, null, false));

        assertThat(systemPrompt.get()).contains("Do not call searchWeb");
        assertThat(userPrompt.get()).contains("Web search is disabled");
    }

    @Test
    void emitsProgressEventsWhenListenerProvided() {
        java.util.ArrayList<AiAgentProgressEvent> events = new java.util.ArrayList<>();
        AiAgentProgressListener listener = new AiAgentProgressListener() {
            @Override
            public void onEvent(AiAgentProgressEvent event) {
                events.add(event);
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        };
        LearningPlanAgentService service = service((config, system, user) -> completion("""
            {"title":"Java 学习计划","items":[{"title":"语法","estimatedHours":1}]}
            """));

        service.generate(new GenerateLearningPlanRequest("Java", null, null, null, null, null), listener);

        assertThat(events).extracting(AiAgentProgressEvent::phase)
            .contains(
                AiAgentProgressEvent.phaseStarted(),
                AiAgentProgressEvent.phaseParsing(),
                AiAgentProgressEvent.phaseCompleted()
            );
    }

    private LearningPlanAgentService service(AiChatClient chatClient) {
        return service(chatClient, null, false);
    }

    private LearningPlanAgentService service(AiChatClient chatClient, AiAgentToolLoopClient toolLoopClient, boolean toolLoopEnabled) {
        AiAgentRunLogEntity runLog = new AiAgentRunLogEntity();
        runLog.setId("run-test");
        AiAgentRunLogService runLogService = mock(AiAgentRunLogService.class);
        when(runLogService.start(anyString(), any(AiAgentRuntimeConfig.class), anyString(), anyString())).thenReturn(runLog);
        when(runLogService.markSuccess(anyString(), any(AiChatCompletionResult.class), anyString())).thenReturn(runLog);
        when(runLogService.markFailed(anyString(), any(), any(Throwable.class))).thenReturn(runLog);
        AiAgentProperties properties = new AiAgentProperties();
        properties.getToolLoop().setEnabled(toolLoopEnabled);
        AiAgentToolLoopClient loopClient = toolLoopClient == null
            ? (config, system, user, context, listener, tools) -> {
                throw new IllegalStateException("tool loop should not be invoked");
            }
            : toolLoopClient;
        return new LearningPlanAgentService(
            chatClient,
            loopClient,
            mock(AiAgentTools.class),
            mock(AiAgentWebSearchTools.class),
            properties,
            () -> runtimeConfig,
            runLogService,
            objectMapper
        );
    }

    private AiAgentProperties enabledToolLoopProperties() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getToolLoop().setEnabled(true);
        return properties;
    }

    private AiAgentProperties disabledToolLoopProperties() {
        AiAgentProperties properties = new AiAgentProperties();
        properties.getToolLoop().setEnabled(false);
        return properties;
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
