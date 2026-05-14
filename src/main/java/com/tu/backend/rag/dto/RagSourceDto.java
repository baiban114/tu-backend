package com.tu.backend.rag.dto;

public record RagSourceDto(
    String kbId,
    String pageId,
    String blockId,
    String title,
    String content,
    String blockType,
    Double score
) {
}
