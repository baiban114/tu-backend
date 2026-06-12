package com.tu.backend.contenttree.dto;

import java.util.List;

public record PageOutlineResponseDto(
    String pageId,
    String kbId,
    String pageTitle,
    List<ContentTreeNodeDto> nodes
) {
}
