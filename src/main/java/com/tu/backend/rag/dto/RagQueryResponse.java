package com.tu.backend.rag.dto;

import java.util.List;

public record RagQueryResponse(
    String answer,
    List<RagSourceDto> sources
) {
}
