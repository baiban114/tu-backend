package com.tu.backend.taskintegration;

public record KaneoConnectionContext(
    String baseUrl,
    String apiKey,
    String workspaceId,
    String adapterProfileJson
) {

    public boolean configured() {
        return baseUrl != null && !baseUrl.isBlank() && apiKey != null && !apiKey.isBlank();
    }
}
