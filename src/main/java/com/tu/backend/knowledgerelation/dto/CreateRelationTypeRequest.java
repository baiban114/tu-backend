package com.tu.backend.knowledgerelation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRelationTypeRequest(
    @NotBlank @Size(max = 64) String typeKey,
    @NotBlank @Size(max = 64) String label,
    @Size(max = 32) String color,
    @Size(max = 64) String icon,
    Boolean bidirectional
) {
}
