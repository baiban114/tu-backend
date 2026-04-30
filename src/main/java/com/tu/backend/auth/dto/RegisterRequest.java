package com.tu.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank
    @Size(min = 3, max = 64)
    String username,

    @NotBlank
    @Email
    @Size(max = 128)
    String email,

    @NotBlank
    @Size(min = 6, max = 72)
    String password,

    @Size(max = 64)
    String displayName
) {
}
