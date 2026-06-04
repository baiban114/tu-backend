package com.tu.backend.externalresource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateResourceItemRequest(
    @NotBlank @Size(max = 64) String typeId,
    @Size(max = 64) String workId,
    @NotBlank @Size(max = 255) String title,
    @Size(max = 512) String identityValue,
    @Size(max = 1024) String sourceUrl,
    @Size(max = 128) String edition,
    @Size(max = 1024) String note,
    @Size(max = 16) String titleSource,
    @Size(max = 16) String workIdSource,
    @Size(max = 32) String variantKind
) {
}
