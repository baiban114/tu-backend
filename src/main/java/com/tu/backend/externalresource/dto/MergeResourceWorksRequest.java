package com.tu.backend.externalresource.dto;

import jakarta.validation.constraints.NotBlank;

public record MergeResourceWorksRequest(
    @NotBlank String sourceWorkId,
    @NotBlank String targetWorkId
) {
}
