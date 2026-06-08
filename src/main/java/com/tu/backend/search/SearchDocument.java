package com.tu.backend.search;

public record SearchDocument(
    String id,
    String kbId,
    String kbName,
    String pageId,
    String pageTitle,
    String blockId,
    String blockType,
    String title,
    String body,
    String updatedAt
) {
}
