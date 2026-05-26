package com.tu.backend.taskintegration.dto;

import jakarta.validation.constraints.Size;

public record MoveExternalTaskRequest(
    @Size(max = 64) String status,
    @Size(max = 128) String columnId,
    Integer position
) {
}
