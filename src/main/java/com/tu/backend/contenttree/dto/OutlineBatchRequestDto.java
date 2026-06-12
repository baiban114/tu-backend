package com.tu.backend.contenttree.dto;

import java.util.List;

public record OutlineBatchRequestDto(
    List<String> pageIds,
    List<String> blockIds
) {
}
