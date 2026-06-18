package com.tu.backend.ai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public record GenerateLearningPlanRequest(
    @NotBlank String topic,
    @DecimalMin(value = "0.0", inclusive = false) Double totalHours,
    @DecimalMin(value = "0.0", inclusive = false) Double dailyHours,
    String deadline,
    String kbId,
    Boolean enableWebSearch
) {
}
