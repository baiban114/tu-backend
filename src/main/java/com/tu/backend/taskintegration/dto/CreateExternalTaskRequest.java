package com.tu.backend.taskintegration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateExternalTaskRequest(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 4096) String description,
    @Size(max = 64) String status,
    @Size(max = 64) String priority,
    @Size(max = 128) String assigneeId,
    @Size(max = 64) String dueDate
) {
}
