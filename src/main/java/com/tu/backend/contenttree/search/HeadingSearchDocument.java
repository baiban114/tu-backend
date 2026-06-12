package com.tu.backend.contenttree.search;

import java.math.BigDecimal;

public record HeadingSearchDocument(
    String id,
    String kbId,
    String pageId,
    String pageTitle,
    String nodeId,
    String sourceBlockId,
    Integer level,
    Integer sortOrder,
    String text,
    String previewText,
    String sourceType,
    BigDecimal estimatedHours,
    BigDecimal totalEstimatedHours,
    String updatedAt
) {
}
