package com.tu.backend.externalresource.dto;

public record ResourceTypeDto(
    String id,
    String code,
    String name,
    String icon,
    String description,
    String identityFieldKey,
    String identityFieldLabel
) {
}
