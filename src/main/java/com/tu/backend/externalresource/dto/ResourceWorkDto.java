package com.tu.backend.externalresource.dto;

public record ResourceWorkDto(
    String id,
    String typeId,
    String typeName,
    String title,
    String subtitle,
    String description,
    String clusterKey,
    String titleSource
) {
}
