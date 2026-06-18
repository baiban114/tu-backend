package com.tu.backend.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AiAgentJsonContent {

    private static final Pattern JSON_FENCE = Pattern.compile(
        "```(?:json)?\\s*\\r?\\n([\\s\\S]*?)\\r?\\n```",
        Pattern.CASE_INSENSITIVE
    );

    private AiAgentJsonContent() {
    }

    static String extract(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimmed;
        }
        Matcher matcher = JSON_FENCE.matcher(trimmed);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        if (trimmed.startsWith("```")) {
            int firstBreak = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstBreak >= 0 && lastFence > firstBreak) {
                return trimmed.substring(firstBreak + 1, lastFence).trim();
            }
        }
        int firstBrace = trimmed.indexOf('{');
        int lastBrace = trimmed.lastIndexOf('}');
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1).trim();
        }
        return trimmed;
    }
}
