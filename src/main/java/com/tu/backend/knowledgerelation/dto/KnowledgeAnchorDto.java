package com.tu.backend.knowledgerelation.dto;

import java.util.Map;

public record KnowledgeAnchorDto(
    String kind,
    String locator,
    Map<String, Object> snapshot
) {
}
