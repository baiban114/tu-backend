package com.tu.backend.contenttree.dto;

import java.util.List;

public record BlockOutlineResponseDto(
    String blockId,
    String pageId,
    List<ContentTreeNodeDto> nodes
) {
}
