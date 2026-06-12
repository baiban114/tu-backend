package com.tu.backend.contenttree.dto;

import java.math.BigDecimal;

public record UpdateContentTreeNodeRequestDto(
    BigDecimal estimatedHours
) {
}
