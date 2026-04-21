package com.tu.backend.content.dto;

import java.util.List;

public record PageContentDto(
    String pageId,
    List<Object> blocks
) {
}
