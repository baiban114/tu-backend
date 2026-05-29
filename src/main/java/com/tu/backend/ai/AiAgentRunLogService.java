package com.tu.backend.ai;

import com.tu.backend.ai.dto.AiAgentRunLogDetailDto;
import com.tu.backend.ai.dto.AiAgentRunLogSummaryDto;
import com.tu.backend.ai.entity.AiAgentRunLogEntity;
import com.tu.backend.ai.repository.AiAgentRunLogRepository;
import com.tu.backend.common.BusinessException;
import com.tu.backend.common.PageResponse;
import jakarta.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class AiAgentRunLogService {

    public static final String STATUS_RUNNING = "running";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_FAILED = "failed";
    public static final String TASK_LEARNING_PLAN = "learning-plan";

    private final AiAgentRunLogRepository repository;

    public AiAgentRunLogService(AiAgentRunLogRepository repository) {
        this.repository = repository;
    }

    public AiAgentRunLogEntity start(
        String taskType,
        AiAgentRuntimeConfig config,
        String systemPrompt,
        String userPrompt
    ) {
        AiAgentRunLogEntity entity = new AiAgentRunLogEntity();
        entity.setId("run-" + UUID.randomUUID());
        entity.setTaskType(taskType);
        entity.setStatus(STATUS_RUNNING);
        entity.setBaseUrl(config.baseUrl());
        entity.setModel(config.model());
        entity.setStartedAt(LocalDateTime.now());
        entity.setSystemPrompt(systemPrompt);
        entity.setUserPrompt(userPrompt);
        return repository.save(entity);
    }

    public AiAgentRunLogEntity markSuccess(
        String id,
        AiChatCompletionResult result,
        String outputText
    ) {
        AiAgentRunLogEntity entity = getEntity(id);
        entity.setStatus(STATUS_SUCCESS);
        entity.setFinishedAt(LocalDateTime.now());
        applyCompletionDetails(entity, result);
        entity.setOutputText(outputText);
        entity.setErrorMessage(null);
        setDurationIfMissing(entity);
        return repository.save(entity);
    }

    public AiAgentRunLogEntity markFailed(
        String id,
        AiChatCompletionResult result,
        Throwable error
    ) {
        AiAgentRunLogEntity entity = getEntity(id);
        entity.setStatus(STATUS_FAILED);
        entity.setFinishedAt(LocalDateTime.now());
        if (result != null) {
            applyCompletionDetails(entity, result);
            entity.setOutputText(result.content());
        }
        if (error instanceof AiChatException chatException) {
            entity.setRequestBodyJson(chatException.getRequestBodyJson());
            entity.setRawResponseBody(chatException.getRawResponseBody());
            entity.setDurationMs(chatException.getDurationMs());
            entity.setPromptTokens(chatException.getPromptTokens());
            entity.setCompletionTokens(chatException.getCompletionTokens());
            entity.setTotalTokens(chatException.getTotalTokens());
        }
        entity.setErrorMessage(error == null ? "" : error.getMessage());
        setDurationIfMissing(entity);
        return repository.save(entity);
    }

    public PageResponse<AiAgentRunLogSummaryDto> list(String taskType, String status, int page, int pageSize) {
        int safePage = Math.max(0, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        var result = repository.findAll(
            filter(taskType, status),
            PageRequest.of(safePage, safePageSize, Sort.by(Sort.Direction.DESC, "startedAt"))
        );
        return PageResponse.of(
            result.getContent().stream().map(this::toSummary).toList(),
            result.getTotalElements(),
            safePage,
            safePageSize
        );
    }

    public AiAgentRunLogDetailDto detail(String id) {
        return toDetail(getEntity(id));
    }

    private AiAgentRunLogEntity getEntity(String id) {
        return repository.findById(id)
            .orElseThrow(() -> new BusinessException(40001, "ai agent run log not found"));
    }

    private void applyCompletionDetails(AiAgentRunLogEntity entity, AiChatCompletionResult result) {
        entity.setRequestBodyJson(result.requestBodyJson());
        entity.setRawResponseBody(result.rawResponseBody());
        entity.setDurationMs(result.durationMs());
        entity.setPromptTokens(result.promptTokens());
        entity.setCompletionTokens(result.completionTokens());
        entity.setTotalTokens(result.totalTokens());
    }

    private void setDurationIfMissing(AiAgentRunLogEntity entity) {
        if (entity.getDurationMs() != null || entity.getStartedAt() == null || entity.getFinishedAt() == null) {
            return;
        }
        entity.setDurationMs(Math.max(0, Duration.between(entity.getStartedAt(), entity.getFinishedAt()).toMillis()));
    }

    private Specification<AiAgentRunLogEntity> filter(String taskType, String status) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (taskType != null && !taskType.isBlank()) {
                predicates.add(builder.equal(root.get("taskType"), taskType.trim()));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(builder.equal(root.get("status"), status.trim()));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private AiAgentRunLogSummaryDto toSummary(AiAgentRunLogEntity entity) {
        return new AiAgentRunLogSummaryDto(
            entity.getId(),
            entity.getTaskType(),
            entity.getStatus(),
            entity.getBaseUrl(),
            entity.getModel(),
            entity.getStartedAt(),
            entity.getFinishedAt(),
            entity.getDurationMs(),
            entity.getPromptTokens(),
            entity.getCompletionTokens(),
            entity.getTotalTokens()
        );
    }

    private AiAgentRunLogDetailDto toDetail(AiAgentRunLogEntity entity) {
        return new AiAgentRunLogDetailDto(
            entity.getId(),
            entity.getTaskType(),
            entity.getStatus(),
            entity.getBaseUrl(),
            entity.getModel(),
            entity.getStartedAt(),
            entity.getFinishedAt(),
            entity.getDurationMs(),
            entity.getSystemPrompt(),
            entity.getUserPrompt(),
            entity.getRequestBodyJson(),
            entity.getRawResponseBody(),
            entity.getOutputText(),
            entity.getErrorMessage(),
            entity.getPromptTokens(),
            entity.getCompletionTokens(),
            entity.getTotalTokens()
        );
    }
}
