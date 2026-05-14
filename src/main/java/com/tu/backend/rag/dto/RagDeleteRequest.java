package com.tu.backend.rag.dto;

import java.util.List;

public record RagDeleteRequest(
    String kbId,
    String pageId,
    List<String> pageIds
) {
}
