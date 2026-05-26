package com.tu.backend.taskintegration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRelationRequest(
    @NotBlank @Size(max = 64) String pageId,
    @Size(max = 1024) String blockId,
    @Size(max = 64) String relationType
) {
}
