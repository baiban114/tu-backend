package com.tu.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank
    @Size(max = 128)
    String account,

    @NotBlank
    @Size(max = 72)
    String password
) {
}
