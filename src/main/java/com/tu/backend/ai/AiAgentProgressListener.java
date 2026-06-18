package com.tu.backend.ai;

public interface AiAgentProgressListener {

    void onEvent(AiAgentProgressEvent event);

    boolean isCancelled();
}
