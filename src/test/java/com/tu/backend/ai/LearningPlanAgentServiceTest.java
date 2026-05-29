package com.tu.backend.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tu.backend.ai.dto.GenerateLearningPlanRequest;
import com.tu.backend.common.BusinessException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class LearningPlanAgentServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void generatesSanitizedLearningPlan() {
        LearningPlanAgentService service = new LearningPlanAgentService((system, user) -> """
            {
              "title": "Java 学习计划",
              "totalEstimatedHours": 99,
              "items": [
                {
                  "title": "基础",
                  "estimatedHours": 10,
                  "children": [
                    {"title": "语法", "estimatedHours": 2.5, "resource": "https://example.com/java"},
                    {"title": "集合", "estimatedHours": 3}
                  ]
                }
              ]
            }
            """, objectMapper);

        var response = service.generate(new GenerateLearningPlanRequest("Java", 12.0, 1.5, null));

        assertThat(response.title()).isEqualTo("Java 学习计划");
        assertThat(response.totalEstimatedHours()).isEqualTo(5.5);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().estimatedHours()).isNull();
        assertThat(response.items().getFirst().children()).hasSize(2);
    }

    @Test
    void rejectsInvalidJson() {
        LearningPlanAgentService service = new LearningPlanAgentService((system, user) -> "{bad", objectMapper);

        assertThatThrownBy(() -> service.generate(new GenerateLearningPlanRequest("Java", null, null, null)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("ai agent returned invalid learning plan json")
            .hasMessageContaining("JsonParseException")
            .hasMessageContaining("rawResponse={bad");
    }

    @Test
    void rejectsNegativeHours() {
        LearningPlanAgentService service = new LearningPlanAgentService((system, user) -> """
            {"title":"x","items":[{"title":"step","estimatedHours":-1}]}
            """, objectMapper);

        assertThatThrownBy(() -> service.generate(new GenerateLearningPlanRequest("Java", null, null, null)))
            .isInstanceOf(BusinessException.class)
            .hasMessage("ai agent returned invalid estimated hours");
    }

    @Test
    void asksAgentToDefaultToChineseOutput() {
        AtomicReference<String> systemPrompt = new AtomicReference<>();
        LearningPlanAgentService service = new LearningPlanAgentService((system, user) -> {
            systemPrompt.set(system);
            return """
                {"title":"Java 学习计划","items":[{"title":"语法","estimatedHours":1}]}
                """;
        }, objectMapper);

        service.generate(new GenerateLearningPlanRequest("Java", null, null, null));

        assertThat(systemPrompt.get()).contains(AiAgentPrompts.DEFAULT_CHINESE_OUTPUT_CONSTRAINT);
    }
}
