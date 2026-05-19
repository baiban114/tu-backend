package com.tu.backend.reference.dto;

public record ReferenceSourceDto(
    String pageId,
    String pageTitle,
    String blockId,
    String blockType,
    String sourceKind,
    String sourceLocator
) {
}
