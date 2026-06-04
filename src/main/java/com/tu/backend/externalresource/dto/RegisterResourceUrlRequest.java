package com.tu.backend.externalresource.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterResourceUrlRequest(
    @NotBlank String url,
    String label,
    String excerptText
) {
}
