package com.tu.backend.storage.dto;

public record FileUploadResponseDto(
    String id,
    String url,
    String contentType,
    long sizeBytes
) {
}
