package com.tu.backend.knowledgerelation.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record GenerateKnowledgePointsRequest(
    @NotEmpty List<String> sources,
    List<String> pageIds
) {
}
