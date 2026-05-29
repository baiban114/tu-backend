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
    private final AiAgentRuntimeConfigResolver configResolver;
    private final AiAgentRunLogService runLogService;
    private final ObjectMapper objectMapper;

    public LearningPlanAgentService(
        AiChatClient chatClient,
        AiAgentRuntimeConfigResolver configResolver,
        AiAgentRunLogService runLogService,
        ObjectMapper objectMapper
    ) {
        this.chatClient = chatClient;
        this.configResolver = configResolver;
        this.runLogService = runLogService;
        this.objectMapper = objectMapper;
    }

    public LearningPlanResponseDto generate(GenerateLearningPlanRequest request) {
        String topic = normalize(request.topic());
        if (topic.isBlank()) {
            throw new BusinessException(40000, "topic is required");
        }
        String systemPrompt = systemPrompt();
        String userPrompt = userPrompt(request, topic);
        AiAgentRuntimeConfig config = configResolver.runtimeConfig();
        AiAgentRunLogEntity runLog = startRunLog(config, systemPrompt, userPrompt);
        AiChatCompletionResult completion = null;
        try {
            completion = chatClient.completeJson(config, systemPrompt, userPrompt);
            LearningPlanResponseDto parsed = parse(completion.content());
            String title = normalize(parsed.title()).isBlank() ? topic + " 学习计划" : normalize(parsed.title());
            List<LearningPlanNodeDto> items = validateNodes(parsed.items(), 1, new Counter());
            if (items.isEmpty()) {
                throw new BusinessException(50324, "ai agent returned empty learning plan");
            }
            double total = roundHours(items.stream().mapToDouble(this::nodeHours).sum());
            LearningPlanResponseDto response = new LearningPlanResponseDto(title, total, items);
            markRunLogSuccess(runLog, completion, serializeOutput(response));
            return response;
        } catch (RuntimeException ex) {
            markRunLogFailed(runLog, completion, ex);
            throw ex;
        }
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
            return objectMapper.readValue(rawJson, LearningPlanResponseDto.class);
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

    private String systemPrompt() {
        return """
            You generate structured learning plans. Return only valid JSON with this shape:
            {"title":"...","totalEstimatedHours":number,"items":[{"title":"...","description":"...","estimatedHours":number,"resource":"...","children":[]}]}
            %s
            Use chapters as parent nodes and concrete study steps as leaf nodes. Parent nodes should omit estimatedHours when they have children.
            Hours must be non-negative. Keep the plan practical and concise.
            """.formatted(AiAgentPrompts.DEFAULT_CHINESE_OUTPUT_CONSTRAINT);
    }

    private String userPrompt(GenerateLearningPlanRequest request, String topic) {
        return """
            Learning goal: %s
            Total available hours: %s
            Daily study hours: %s
            Deadline: %s
            Generate a chapter-based multi-level learning plan.
            """.formatted(
            topic,
            request.totalHours() == null ? "not specified" : request.totalHours(),
            request.dailyHours() == null ? "not specified" : request.dailyHours(),
            normalize(request.deadline()).isBlank() ? "not specified" : normalize(request.deadline())
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
