package com.tu.backend.auth.dto;

public record AuthResponse(
    UserDto user,
    String tokenType
) {
}
