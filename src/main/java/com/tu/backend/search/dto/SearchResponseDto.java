package com.tu.backend.search.dto;

import java.util.List;

public record SearchResponseDto(
    List<SearchHitDto> hits,
    boolean enabled,
    String message
) {
}
