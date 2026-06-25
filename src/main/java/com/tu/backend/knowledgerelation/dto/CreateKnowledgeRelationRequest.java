package com.tu.backend.knowledgerelation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKnowledgeRelationRequest(
    @NotBlank @Size(max = 64) String relationTypeKey,
    @Size(max = 64) String fromPointId,
    @Size(max = 64) String toPointId,
    KnowledgeAnchorDto from,
    KnowledgeAnchorDto to,
    @Size(max = 1024) String note
) {
}
