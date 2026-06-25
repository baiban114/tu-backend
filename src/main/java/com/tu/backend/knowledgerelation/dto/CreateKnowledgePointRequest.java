package com.tu.backend.knowledgerelation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKnowledgePointRequest(
    @Size(max = 64) String parentId,
    @NotBlank @Size(max = 255) String title,
    @Size(max = 4000) String summary,
    Double estimatedHours,
    KnowledgeAnchorDto sourceAnchor
) {
}
