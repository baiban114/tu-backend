package com.tu.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiAgentJsonContentTest {

    @Test
    void extractsJsonFromMarkdownFenceAfterProse() {
        String raw = """
            根据我的知识和对 Java 高级面试的全面了解，以下是为期两周的密集学习计划。

            ---

            ```json
            {"title":"Java 面试计划","totalEstimatedHours":80,"items":[]}
            ```
            """;

        assertThat(AiAgentJsonContent.extract(raw))
            .isEqualTo("{\"title\":\"Java 面试计划\",\"totalEstimatedHours\":80,\"items\":[]}");
    }

    @Test
    void returnsPureJsonUnchanged() {
        assertThat(AiAgentJsonContent.extract("{\"ok\":true}")).isEqualTo("{\"ok\":true}");
    }
}
