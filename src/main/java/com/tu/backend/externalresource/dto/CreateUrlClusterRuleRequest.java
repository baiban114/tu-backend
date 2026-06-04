package com.tu.backend.externalresource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUrlClusterRuleRequest(
    @NotBlank String domain,
    @NotBlank String pathRegex,
    @NotBlank String clusterKeyFormat,
    Integer variantGroup,
    @NotNull Integer priority,
    Boolean enabled,
    String description
) {
}
