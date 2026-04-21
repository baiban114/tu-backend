package com.tu.backend.block.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateBlockGraphRequest(
    @NotBlank String pageId,
    @NotNull Object graphData
) {
}
