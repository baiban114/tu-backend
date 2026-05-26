package com.tu.backend.taskintegration.dto;

public record ExternalProjectDto(
    String provider,
    String externalId,
    String name,
    String description,
    String sourceUrl
) {
}
