package com.tu.backend.common;

public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(0, "ok", null);
    }

    public static ApiResponse<Void> failure(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}

