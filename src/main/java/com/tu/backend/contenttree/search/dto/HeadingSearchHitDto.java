package com.tu.backend.contenttree.search.dto;

import java.math.BigDecimal;

public record HeadingSearchHitDto(
    String nodeId,
    String pageId,
    String pageTitle,
    String kbId,
    String sourceBlockId,
    Integer level,
    String text,
    String highlight,
    String previewText,
    BigDecimal estimatedHours,
    BigDecimal totalEstimatedHours
) {
}
