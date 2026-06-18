package com.tu.backend.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class SseAiAgentProgressListener implements AiAgentProgressListener {

    private static final Logger log = LoggerFactory.getLogger(SseAiAgentProgressListener.class);

    private final ObjectMapper objectMapper;
    private final SseEmitter emitter;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public SseAiAgentProgressListener(ObjectMapper objectMapper, SseEmitter emitter) {
        this.objectMapper = objectMapper;
        this.emitter = emitter;
        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> cancelled.set(true));
        emitter.onError(ex -> cancelled.set(true));
    }

    @Override
    public void onEvent(AiAgentProgressEvent event) {
        if (cancelled.get()) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                .name(event.phase())
                .data(objectMapper.writeValueAsString(event)));
        } catch (IOException ex) {
            cancelled.set(true);
            log.debug("failed to send ai agent progress event; phase={}", event.phase(), ex);
        } catch (IllegalStateException ex) {
            cancelled.set(true);
            log.debug("sse emitter already closed; phase={}", event.phase(), ex);
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }
}
