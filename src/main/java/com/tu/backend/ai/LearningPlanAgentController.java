package com.tu.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.ai.dto.GenerateLearningPlanRequest;
import com.tu.backend.ai.dto.LearningPlanResponseDto;
import com.tu.backend.common.BusinessException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai/learning-plan")
public class LearningPlanAgentController {

    private static final Logger log = LoggerFactory.getLogger(LearningPlanAgentController.class);

    private final LearningPlanAgentService service;
    private final ObjectMapper objectMapper;

    public LearningPlanAgentController(LearningPlanAgentService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/generate")
    public com.tu.backend.common.ApiResponse<LearningPlanResponseDto> generate(
        @Valid @RequestBody GenerateLearningPlanRequest request
    ) {
        return com.tu.backend.common.ApiResponse.success(service.generate(request));
    }

    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateStream(@Valid @RequestBody GenerateLearningPlanRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        SseAiAgentProgressListener listener = new SseAiAgentProgressListener(objectMapper, emitter);
        Thread.startVirtualThread(() -> {
            try {
                service.generate(request, listener);
                emitter.complete();
            } catch (BusinessException ex) {
                if (ex.getCode() != 50326) {
                    log.warn("learning plan stream failed: {}", ex.getMessage());
                }
                try {
                    emitter.complete();
                } catch (Exception completeEx) {
                    log.debug("failed to complete sse emitter after business error", completeEx);
                }
            } catch (Exception ex) {
                log.error("learning plan stream failed", ex);
                emitter.completeWithError(ex);
            }
        });
        return emitter;
    }
}
