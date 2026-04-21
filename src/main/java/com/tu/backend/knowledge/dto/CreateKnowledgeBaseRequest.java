package com.tu.backend.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateKnowledgeBaseRequest(
    @NotBlank @Size(max = 128) String name,
    @Size(max = 32) String icon,
    @Size(max = 255) String description
) {
}

