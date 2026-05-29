package com.tu.backend.ai.dto;

import java.util.List;

public record LearningPlanNodeDto(
    String title,
    String description,
    Double estimatedHours,
    String resource,
    List<LearningPlanNodeDto> children
) {
}
