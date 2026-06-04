package com.tu.backend.externalresource.dto;

public record UrlClusterRuleDto(
    String id,
    String domain,
    String pathRegex,
    String clusterKeyFormat,
    Integer variantGroup,
    int priority,
    boolean enabled,
    boolean builtIn,
    String description
) {
}
