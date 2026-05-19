package com.tu.backend.reference.dto;

public record ReferenceItemDto(
    String id,
    String category,
    boolean editable,
    ReferenceSourceDto source,
    ReferenceTargetDto target,
    String status,
    ReferenceCitationDto citation
) {
}
