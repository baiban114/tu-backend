package com.tu.backend.block.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateBlockContentRequest(
    @NotBlank String pageId,
    String content
) {
}

