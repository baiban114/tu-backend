package com.tu.backend.externalresource.dto;

public record ResourceExcerptDto(
    String id,
    String resourceItemId,
    String resourceItemTitle,
    String title,
    String locator,
    String excerptText,
    String note,
    Integer sortOrder
) {
}
