package com.tu.backend.knowledgerelation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateKnowledgePointAnchorRequest(
    @NotNull KnowledgeAnchorDto anchor,
    @Size(max = 32) String role,
    Boolean primary
) {
}
