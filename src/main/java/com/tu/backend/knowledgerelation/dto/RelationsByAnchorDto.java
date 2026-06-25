package com.tu.backend.knowledgerelation.dto;

import java.util.List;

public record RelationsByAnchorDto(
    String locator,
    List<KnowledgeRelationDto> outgoing,
    List<KnowledgeRelationDto> incoming
) {
}
