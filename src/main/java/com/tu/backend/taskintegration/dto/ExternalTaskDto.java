package com.tu.backend.taskintegration.dto;

public record ExternalTaskDto(
    String provider,
    String externalId,
    String projectId,
    String number,
    String title,
    String description,
    String status,
    String priority,
    String assigneeName,
    String dueDate,
    Integer position,
    String sourceUrl,
    String updatedAt
) {
}
