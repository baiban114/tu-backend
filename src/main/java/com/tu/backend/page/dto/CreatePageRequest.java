package com.tu.backend.page.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePageRequest(
    @NotBlank String kbId,
    String parentId,
    @Size(max = 128) String title,
    @Size(max = 32) String pageType
) {
}

