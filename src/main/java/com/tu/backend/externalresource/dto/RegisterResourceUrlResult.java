package com.tu.backend.externalresource.dto;

public record RegisterResourceUrlResult(
    String mode,
    ResourceItemDto item,
    ResourceExcerptDto excerpt,
    boolean createdItem,
    boolean createdExcerpt
) {
}
