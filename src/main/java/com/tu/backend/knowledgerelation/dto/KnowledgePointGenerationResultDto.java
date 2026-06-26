package com.tu.backend.knowledgerelation.dto;

import java.util.List;

public record KnowledgePointGenerationResultDto(
    int created,
    int skipped,
    int failed,
    List<KnowledgePointGenerationItemDto> items
) {
}
