package com.tu.backend.knowledgerelation.dto;

import jakarta.validation.constraints.Size;

public record UpdateKnowledgePointRequest(
    @Size(max = 64) String parentId,
    @Size(max = 255) String title,
    @Size(max = 4000) String summary,
    @Size(max = 16) String status,
    Double estimatedHours,
    Integer sortOrder
) {
}
