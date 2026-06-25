package com.tu.backend.knowledgerelation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateKnowledgeRelationRequest(
    @NotBlank @Size(max = 64) String relationTypeKey,
    @NotNull KnowledgeAnchorDto from,
    @NotNull KnowledgeAnchorDto to,
    @Size(max = 1024) String note
) {
}
