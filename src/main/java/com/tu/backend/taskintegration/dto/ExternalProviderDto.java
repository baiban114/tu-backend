package com.tu.backend.taskintegration.dto;

public record ExternalProviderDto(
    String id,
    String name,
    String license,
    boolean configured
) {
}
