package com.tu.backend.rag.dto;

import java.util.List;

public record RagIndexRequest(
    String kbId,
    String pageId,
    List<RagIndexDocument> documents
) {
}
