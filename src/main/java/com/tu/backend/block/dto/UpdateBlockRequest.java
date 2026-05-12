package com.tu.backend.block.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateBlockRequest(
    @NotBlank String pageId,
    @NotNull Object block
) {
}
