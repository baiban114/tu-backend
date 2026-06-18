package com.tu.backend.ai;

public final class AiAgentExecutionContextHolder {

    private static final ThreadLocal<AiAgentExecutionContext> CONTEXT = new ThreadLocal<>();

    private AiAgentExecutionContextHolder() {
    }

    public static void set(AiAgentExecutionContext context) {
        CONTEXT.set(context);
    }

    public static AiAgentExecutionContext get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
