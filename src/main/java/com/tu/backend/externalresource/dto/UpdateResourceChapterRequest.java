package com.tu.backend.externalresource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateResourceChapterRequest(
    @Size(max = 64) String parentId,
    @NotBlank @Size(max = 255) String title,
    @Size(max = 255) String locator,
    @Size(max = 1024) String note,
    Integer sortOrder
) {
}
