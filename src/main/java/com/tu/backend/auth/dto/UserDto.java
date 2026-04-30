package com.tu.backend.auth.dto;

import java.time.LocalDateTime;

public record UserDto(
    String id,
    String username,
    String email,
    String displayName,
    LocalDateTime createdAt
) {
}
