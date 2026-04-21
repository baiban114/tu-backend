package com.tu.backend.block.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SyncBlocksRequest(
    @NotBlank String pageId,
    @NotNull List<Object> blocks
) {
}
