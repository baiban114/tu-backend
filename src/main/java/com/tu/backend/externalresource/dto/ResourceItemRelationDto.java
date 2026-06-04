package com.tu.backend.externalresource.dto;

public record ResourceItemRelationDto(
    String id,
    String fromItemId,
    String fromItemTitle,
    String toItemId,
    String toItemTitle,
    String relationType,
    String note
) {
}
