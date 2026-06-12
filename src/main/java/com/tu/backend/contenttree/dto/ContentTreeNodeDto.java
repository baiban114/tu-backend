package com.tu.backend.contenttree.dto;

import java.math.BigDecimal;

public record ContentTreeNodeDto(
    String id,
    String scopeType,
    String scopeId,
    String parentId,
    String title,
    Integer sortOrder,
    BigDecimal estimatedHours,
    BigDecimal totalEstimatedHours,
    String locator,
    String note,
    String sourceBlockId,
    Integer level,
    String sourceType,
    String previewText,
    String blockType
) {
}
