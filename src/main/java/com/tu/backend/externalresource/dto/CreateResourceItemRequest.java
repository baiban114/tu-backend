package com.tu.backend.externalresource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateResourceItemRequest(
    @NotBlank @Size(max = 64) String typeId,
    @NotBlank @Size(max = 64) String workId,
    @NotBlank @Size(max = 255) String title,
    @NotBlank @Size(max = 512) String identityValue,
    @Size(max = 1024) String sourceUrl,
    @Size(max = 128) String edition,
    @Size(max = 1024) String note
) {
}
