package com.tu.backend.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record RagQueryRequest(
    @NotBlank String kbId,
    @NotBlank String query,
    Integer topK
) {
}
