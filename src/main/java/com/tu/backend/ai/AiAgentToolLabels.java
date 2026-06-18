package com.tu.backend.ai;

import java.util.Map;

final class AiAgentToolLabels {

    private static final Map<String, String> LABELS = Map.of(
        "searchKnowledgeBasePages", "搜索知识库",
        "queryKnowledgeBaseRag", "语义检索（RAG）",
        "searchWeb", "联网搜索"
    );

    private AiAgentToolLabels() {
    }

    static String label(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return "工具";
        }
        return LABELS.getOrDefault(toolName, toolName);
    }

    static String toolCallMessage(String toolName) {
        return "正在" + label(toolName) + "…";
    }

    static String toolDoneMessage(String toolName) {
        return label(toolName) + " 完成";
    }
}
