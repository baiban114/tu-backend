package com.tu.backend.contenttree.dto;

import java.util.List;

public record OutlineBatchResponseDto(
    List<PageOutlineResponseDto> pages,
    List<BlockOutlineResponseDto> blocks
) {
}
