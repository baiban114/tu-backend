package com.tu.backend.knowledgerelation.dto;

import java.util.List;

public record RelationsByPointDto(
    String pointId,
    List<KnowledgeRelationDto> outgoing,
    List<KnowledgeRelationDto> incoming
) {
}
