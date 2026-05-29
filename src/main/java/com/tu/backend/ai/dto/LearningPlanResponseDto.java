package com.tu.backend.ai.dto;

import java.util.List;

public record LearningPlanResponseDto(
    String title,
    Double totalEstimatedHours,
    List<LearningPlanNodeDto> items
) {
}
