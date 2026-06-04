package com.tu.backend.externalresource.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateResourceItemRelationRequest(
    @NotBlank String fromItemId,
    @NotBlank String toItemId,
    @NotBlank String relationType,
    String note
) {
}
