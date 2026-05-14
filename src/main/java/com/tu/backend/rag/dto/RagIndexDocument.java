package com.tu.backend.rag.dto;

import java.util.Map;

public record RagIndexDocument(
    String kbId,
    String pageId,
    String blockId,
    String title,
    String content,
    String blockType,
    String updatedAt,
    Map<String, Object> metadata
) {
}
