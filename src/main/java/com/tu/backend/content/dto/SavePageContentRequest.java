package com.tu.backend.content.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SavePageContentRequest(
    @NotNull List<Object> blocks
) {
}
