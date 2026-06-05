package com.tu.backend.externalresource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateResourceExcerptRequest(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 64) String chapterId,
    @Size(max = 255) String locator,
    @Size(max = 20000) String excerptText,
    @Size(max = 1024) String note,
    Integer sortOrder
) {
}
