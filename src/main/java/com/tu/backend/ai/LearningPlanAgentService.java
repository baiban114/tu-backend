package com.tu.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.ai.dto.GenerateLearningPlanRequest;
import com.tu.backend.ai.dto.LearningPlanNodeDto;
import com.tu.backend.ai.dto.LearningPlanResponseDto;
import com.tu.backend.ai.entity.AiAgentRunLogEntity;
import com.tu.backend.common.BusinessException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LearningPlanAgentService {

    private static final Logger log = LoggerFactory.getLogger(LearningPlanAgentService.class);
    private static final int MAX_NODES = 80;
    private static final int MAX_DEPTH = 6;

    private final AiChatClient chatClient;
    private final AiAgentToolLoopClient toolLoopClient;
    private final AiAgentTools aiAgentTools;
    private final AiAgentWebSearchTools aiAgentWebSearchTools;
    private final AiAgentProperties aiAgentProperties;
    private final AiAgentRuntimeConfigResolver configResolver;
    private final AiAgentRunLogService runLogService;
    private final ObjectMapper objectMapper;

    public LearningPlanAgentService(
        AiChatClient chatClient,
        AiAgentToolLoopClient toolLoopClient,
        AiAgentTools aiAgentTools,
        AiAgentWebSearchTools aiAgentWebSearchTools,
        AiAgentProperties aiAgentProperties,
        AiAgentRuntimeConfigResolver configResolver,
        AiAgentRunLogService runLogService,
        ObjectMapper objectMapper
    ) {
        this.chatClient = chatClient;
        this.toolLoopClient = toolLoopClient;
        this.aiAgentTools = aiAgentTools;
        this.aiAgentWebSearchTools = aiAgentWebSearchTools;
        this.aiAgentProperties = aiAgentProperties;
        this.configResolver = configResolver;
        this.runLogService = runLogService;
        this.objectMapper = objectMapper;
    }

    public LearningPlanResponseDto generate(GenerateLearningPlanRequest request) {
        return generate(request, null);
    }

    public LearningPlanResponseDto generate(GenerateLearningPlanRequest request, AiAgentProgressListener progressListener) {
        String topic = normalize(request.topic());
        if (topic.isBlank()) {
            throw new BusinessException(40000, "topic is required");
        }
        long startedAt = System.nanoTime();
        emitProgress(progressListener, AiAgentProgressEvent.of(
            AiAgentProgressEvent.phaseStarted(),
            "开始生成学习计划",
            null,
            null,
            startedAt
        ));
        boolean enableWebSearch = Boolean.TRUE.equals(request.enableWebSearch());
        String systemPrompt = systemPrompt(aiAgentProperties.getToolLoop().isEnabled(), enableWebSearch);
        String userPrompt = userPrompt(request, topic, enableWebSearch);
        AiAgentRuntimeConfig config = configResolver.runtimeConfig();
        AiAgentRunLogEntity runLog = startRunLog(config, systemPrompt, userPrompt);
        AiChatCompletionResult completion = null;
        try {
            ensureNotCancelled(progressListener);
            if (aiAgentProperties.getToolLoop().isEnabled()) {
                AiAgentToolLoopResult loopResult = toolLoopClient.runToolLoop(
                    config,
                    systemPrompt,
                    userPrompt,
                    new AiAgentExecutionContext(blankToNull(request.kbId()), topic, enableWebSearch),
                    progressListener,
                    resolveToolProviders(enableWebSearch)
                );
                completion = loopResult.toCompletionResult();
            } else {
                completion = chatClient.completeJson(config, systemPrompt, userPrompt);
            }
            ensureNotCancelled(progressListener);
            emitProgress(progressListener, AiAgentProgressEvent.of(
                AiAgentProgressEvent.phaseParsing(),
                "正在整理学习计划…",
                null,
                null,
                startedAt
            ));
            LearningPlanResponseDto parsed = parse(completion.content());
            String title = normalize(parsed.title()).isBlank() ? topic + " 学习计划" : normalize(parsed.title());
            List<LearningPlanNodeDto> items = validateNodes(parsed.items(), 1, new Counter());
            if (items.isEmpty()) {
                throw new BusinessException(50324, "ai agent returned empty learning plan");
            }
            double total = roundHours(items.stream().mapToDouble(this::nodeHours).sum());
            LearningPlanResponseDto response = new LearningPlanResponseDto(title, total, items);
            markRunLogSuccess(runLog, completion, serializeOutput(response));
            emitProgress(progressListener, AiAgentProgressEvent.completed(
                "学习计划生成完成",
                startedAt,
                response
            ));
            return response;
        } catch (RuntimeException ex) {
            markRunLogFailed(runLog, completion, ex);
            emitFailure(progressListener, ex, startedAt);
            throw ex;
        }
    }

    private void ensureNotCancelled(AiAgentProgressListener progressListener) {
        if (progressListener != null && progressListener.isCancelled()) {
            throw new BusinessException(50326, "ai agent run cancelled");
        }
    }

    private void emitProgress(AiAgentProgressListener progressListener, AiAgentProgressEvent event) {
        if (progressListener != null) {
            progressListener.onEvent(event);
        }
    }

    private void emitFailure(AiAgentProgressListener progressListener, RuntimeException ex, long startedAt) {
        if (progressListener == null) {
            return;
        }
        if (ex instanceof BusinessException businessException && businessException.getCode() == 50326) {
            emitProgress(progressListener, AiAgentProgressEvent.of(
                AiAgentProgressEvent.phaseCancelled(),
                "已中止生成",
                null,
                null,
                startedAt
            ));
            return;
        }
        emitProgress(progressListener, AiAgentProgressEvent.of(
            AiAgentProgressEvent.phaseFailed(),
            nullToBlank(ex.getMessage()),
            null,
            null,
            startedAt
        ));
    }

    private AiAgentRunLogEntity startRunLog(
        AiAgentRuntimeConfig config,
        String systemPrompt,
        String userPrompt
    ) {
        try {
            return runLogService.start(AiAgentRunLogService.TASK_LEARNING_PLAN, config, systemPrompt, userPrompt);
        } catch (RuntimeException ex) {
            log.error("failed to start ai agent run log; taskType={}", AiAgentRunLogService.TASK_LEARNING_PLAN, ex);
            return null;
        }
    }

    private void markRunLogSuccess(
        AiAgentRunLogEntity runLog,
        AiChatCompletionResult completion,
        String output
    ) {
        if (runLog == null) {
            return;
        }
        try {
            runLogService.markSuccess(runLog.getId(), completion, output);
        } catch (RuntimeException ex) {
            log.error("failed to persist ai agent run success log; runId={}", runLog.getId(), ex);
        }
    }

    private void markRunLogFailed(
        AiAgentRunLogEntity runLog,
        AiChatCompletionResult completion,
        RuntimeException original
    ) {
        if (runLog == null) {
            return;
        }
        try {
            runLogService.markFailed(runLog.getId(), completion, original);
        } catch (RuntimeException logException) {
            log.error(
                "failed to persist ai agent run failure log; runId={}; originalException={}: {}",
                runLog.getId(),
                original.getClass().getName(),
                nullToBlank(original.getMessage()),
                logException
            );
        }
    }

    private LearningPlanResponseDto parse(String rawJson) {
        try {
            return objectMapper.readValue(AiAgentJsonContent.extract(rawJson), LearningPlanResponseDto.class);
        } catch (Exception ex) {
            throw new BusinessException(
                50324,
                "ai agent returned invalid learning plan json: exception=" + ex.getClass().getName()
                    + ": " + nullToBlank(ex.getMessage())
                    + "; rawResponse=" + abbreviate(rawJson)
            );
        }
    }

    private List<LearningPlanNodeDto> validateNodes(List<LearningPlanNodeDto> nodes, int depth, Counter counter) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        if (depth > MAX_DEPTH) {
            throw new BusinessException(50324, "ai agent returned too deep learning plan");
        }
        List<LearningPlanNodeDto> sanitized = new ArrayList<>();
        for (LearningPlanNodeDto node : nodes) {
            counter.increment();
            if (counter.value() > MAX_NODES) {
                throw new BusinessException(50324, "ai agent returned too many learning plan nodes");
            }
            String title = normalize(node.title());
            if (title.isBlank()) {
                throw new BusinessException(50324, "ai agent returned a learning plan node without title");
            }
            List<LearningPlanNodeDto> children = validateNodes(node.children(), depth + 1, counter);
            Double estimatedHours = children.isEmpty() ? sanitizeHours(node.estimatedHours()) : null;
            sanitized.add(new LearningPlanNodeDto(
                title,
                blankToNull(node.description()),
                estimatedHours,
                blankToNull(node.resource()),
                children
            ));
        }
        return sanitized;
    }

    private Double sanitizeHours(Double value) {
        if (value == null) {
            return 0.0;
        }
        if (!Double.isFinite(value) || value < 0) {
            throw new BusinessException(50324, "ai agent returned invalid estimated hours");
        }
        return roundHours(value);
    }

    private double nodeHours(LearningPlanNodeDto node) {
        double own = node.estimatedHours() == null ? 0 : node.estimatedHours();
        double children = node.children() == null
            ? 0
            : node.children().stream().mapToDouble(this::nodeHours).sum();
        return own + children;
    }

    private double roundHours(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String blankToNull(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? null : normalized;
    }

    private String nullToBlank(String value) {
        return value == null ? "" : value;
    }

    private String abbreviate(String value) {
        String normalized = value == null ? "" : value.strip();
        int maxLength = 4000;
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength) + "...<truncated " + (normalized.length() - maxLength) + " chars>";
    }

    private String serializeOutput(LearningPlanResponseDto response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception ex) {
            return "";
        }
    }

    private Object[] resolveToolProviders(boolean enableWebSearch) {
        if (enableWebSearch) {
            return new Object[] { aiAgentTools, aiAgentWebSearchTools };
        }
        return new Object[] { aiAgentTools };
    }

    private String systemPrompt(boolean toolLoopEnabled, boolean enableWebSearch) {
        String toolGuide = toolLoopEnabled
            ? (enableWebSearch
                ? """
            You may call tools to search the user's knowledge base, run semantic RAG retrieval (when kbId is provided), or searchWeb for public information before generating the plan.
            When using searchWeb, look for tutorials, official documentation, courses, and other study materials relevant to the learning goal; prefer authoritative sources when citing resources.
            After gathering enough context, return ONLY valid JSON with this shape:
            """
                : """
            You may call tools to search the user's knowledge base or run semantic RAG retrieval (when kbId is provided) before generating the plan. Do not call searchWeb.
            After gathering enough context, return ONLY valid JSON with this shape:
            """)
            : "Return only valid JSON with this shape:";
        return """
            You generate structured learning plans. %s
            {"title":"...","totalEstimatedHours":number,"items":[{"title":"...","description":"...","estimatedHours":number,"resource":"...","children":[]}]}
            %s
            Use chapters as parent nodes and concrete study steps as leaf nodes. Parent nodes should omit estimatedHours when they have children.
            Hours must be non-negative. Keep the plan practical and concise. Prefer citing resources discovered via tools when available.
            """.formatted(toolGuide, AiAgentPrompts.DEFAULT_CHINESE_OUTPUT_CONSTRAINT);
    }

    private String userPrompt(GenerateLearningPlanRequest request, String topic, boolean enableWebSearch) {
        String webSearchHint = enableWebSearch
            ? "Web search is enabled: use searchWeb to find tutorials, documentation, and reference materials for this learning goal."
            : "Web search is disabled: do not call searchWeb.";
        return """
            Learning goal: %s
            Total available hours: %s
            Daily study hours: %s
            Deadline: %s
            Knowledge base id (for RAG): %s
            %s
            Generate a chapter-based multi-level learning plan.
            """.formatted(
            topic,
            request.totalHours() == null ? "not specified" : request.totalHours(),
            request.dailyHours() == null ? "not specified" : request.dailyHours(),
            normalize(request.deadline()).isBlank() ? "not specified" : normalize(request.deadline()),
            normalize(request.kbId()).isBlank() ? "not specified" : normalize(request.kbId()),
            webSearchHint
        );
    }

    private static final class Counter {
        private int value;

        void increment() {
            value += 1;
        }

        int value() {
            return value;
        }
    }
}
