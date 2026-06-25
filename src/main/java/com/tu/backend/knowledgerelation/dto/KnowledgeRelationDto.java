package com.tu.backend.knowledgerelation.dto;

public record KnowledgeRelationDto(
    String id,
    String kbId,
    String relationTypeKey,
    String relationTypeLabel,
    String relationTypeColor,
    boolean bidirectional,
    String fromPointId,
    String toPointId,
    String fromPointTitle,
    String toPointTitle,
    KnowledgeAnchorDto from,
    KnowledgeAnchorDto to,
    String note,
    String sourceProvenance,
    String status
) {
}
