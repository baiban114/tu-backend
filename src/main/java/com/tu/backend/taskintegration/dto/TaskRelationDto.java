package com.tu.backend.taskintegration.dto;

public record TaskRelationDto(
    String id,
    String provider,
    String externalId,
    String resourceItemId,
    String pageId,
    String blockId,
    String relationType
) {
}
