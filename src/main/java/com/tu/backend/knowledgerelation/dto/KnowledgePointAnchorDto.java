package com.tu.backend.knowledgerelation.dto;

import java.util.Map;

public record KnowledgePointAnchorDto(
    String id,
    String knowledgePointId,
    String kind,
    String locator,
    Map<String, Object> snapshot,
    String role,
    boolean primary
) {
}
