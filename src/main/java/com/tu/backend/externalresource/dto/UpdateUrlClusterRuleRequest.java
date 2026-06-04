package com.tu.backend.externalresource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUrlClusterRuleRequest(
    @NotBlank String domain,
    @NotBlank String pathRegex,
    @NotBlank String clusterKeyFormat,
    Integer variantGroup,
    @NotNull Integer priority,
    @NotNull Boolean enabled,
    String description
) {
}
