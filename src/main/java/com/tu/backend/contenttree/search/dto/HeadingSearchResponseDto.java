package com.tu.backend.contenttree.search.dto;

import java.util.List;

public record HeadingSearchResponseDto(
    List<HeadingSearchHitDto> items
) {
}
