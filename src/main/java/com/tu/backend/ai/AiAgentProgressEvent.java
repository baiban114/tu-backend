package com.tu.backend.ai;

import com.tu.backend.ai.dto.LearningPlanResponseDto;

public record AiAgentProgressEvent(
    String phase,
    String message,
    Integer round,
    String toolName,
    Long elapsedMs,
    LearningPlanResponseDto result
) {

    public static AiAgentProgressEvent of(
        String phase,
        String message,
        Integer round,
        String toolName,
        long startedAtNanos
    ) {
        return new AiAgentProgressEvent(
            phase,
            message,
            round,
            toolName,
            elapsedMillis(startedAtNanos),
            null
        );
    }

    public static AiAgentProgressEvent completed(String message, long startedAtNanos, LearningPlanResponseDto result) {
        return new AiAgentProgressEvent(
            phaseCompleted(),
            message,
            null,
            null,
            elapsedMillis(startedAtNanos),
            result
        );
    }

    public static String phaseStarted() {
        return "started";
    }

    public static String phaseModelCall() {
        return "model_call";
    }

    public static String phaseToolCall() {
        return "tool_call";
    }

    public static String phaseToolDone() {
        return "tool_done";
    }

    public static String phaseParsing() {
        return "parsing";
    }

    public static String phaseCompleted() {
        return "completed";
    }

    public static String phaseFailed() {
        return "failed";
    }

    public static String phaseCancelled() {
        return "cancelled";
    }

    private static long elapsedMillis(long startedAtNanos) {
        return Math.max(0, Math.round((System.nanoTime() - startedAtNanos) / 1_000_000.0));
    }
}
