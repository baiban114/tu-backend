package com.tu.backend.knowledgerelation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKnowledgePointAliasRequest(
    @NotBlank @Size(max = 255) String alias
) {
}
