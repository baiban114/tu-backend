package com.tu.backend.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateKnowledgeBaseRequest(
    @NotBlank @Size(max = 128) String name
) {
}

