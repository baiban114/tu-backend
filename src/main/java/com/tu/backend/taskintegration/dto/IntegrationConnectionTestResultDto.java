package com.tu.backend.taskintegration.dto;

public record IntegrationConnectionTestResultDto(
    boolean ok,
    String message
) {
}
