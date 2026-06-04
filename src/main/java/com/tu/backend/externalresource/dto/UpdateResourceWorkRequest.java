package com.tu.backend.externalresource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateResourceWorkRequest(
    @NotBlank @Size(max = 64) String typeId,
    @NotBlank @Size(max = 255) String title,
    @Size(max = 255) String subtitle,
    @Size(max = 1024) String description,
    @Size(max = 512) String clusterKey,
    @Size(max = 16) String titleSource
) {
}
