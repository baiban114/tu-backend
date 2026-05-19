package com.tu.backend.reference.dto;

public record ReferenceTargetDto(
    String kind,
    String pageId,
    String pageTitle,
    String blockId,
    String blockPreview,
    String resourceItemId,
    String resourceItemTitle,
    String resourceTypeName,
    String url
) {
}
