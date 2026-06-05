package com.tu.backend.externalresource.dto;

public record ResourceChapterDto(
    String id,
    String resourceItemId,
    String resourceItemTitle,
    String parentId,
    String title,
    String locator,
    String note,
    Integer sortOrder
) {
}
