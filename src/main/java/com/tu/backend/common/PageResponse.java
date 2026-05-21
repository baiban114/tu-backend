package com.tu.backend.common;

import java.util.List;

public record PageResponse<T>(List<T> items, long total, int page, int pageSize) {

    public static <T> PageResponse<T> of(List<T> items, long total, int page, int pageSize) {
        return new PageResponse<>(items, total, page, pageSize);
    }
}
