package com.tu.backend.externalresource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateResourceTypeRequest(
    @NotBlank @Size(max = 128) String name,
    @Size(max = 32) String icon,
    @Size(max = 255) String description,
    @NotBlank @Size(max = 64) String identityFieldKey,
    @NotBlank @Size(max = 128) String identityFieldLabel
) {
}
