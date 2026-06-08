package com.tu.backend.search.dto;

public record SearchHitDto(
    String kbId,
    String kbName,
    String pageId,
    String pageTitle,
    String blockId,
    String blockType,
    String title,
    String snippet
) {
}
