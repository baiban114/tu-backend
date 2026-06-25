package com.tu.backend.knowledgerelation.dto;

public record RelationTypeDefDto(
    String id,
    String kbId,
    String typeKey,
    String label,
    String color,
    String icon,
    boolean bidirectional,
    boolean system,
    boolean enabled
) {
}
